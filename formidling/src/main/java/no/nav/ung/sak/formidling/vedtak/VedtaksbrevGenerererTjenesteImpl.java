package no.nav.ung.sak.formidling.vedtak;


import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.microsoftgraph.MicrosoftGraphTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;
import no.nav.ung.sak.formidling.template.dto.felles.BrevAnsvarligDto;
import no.nav.ung.sak.formidling.template.dto.felles.BrevAnsvarligeDto;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class VedtaksbrevGenerererTjenesteImpl implements VedtaksbrevGenerererTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevGenerererTjenesteImpl.class);
    private static final List<BehandlingDel> REKKEFØLGE_I_BREV = List.of(BehandlingDel.NAV_KONTOR, BehandlingDel.SENTRAL);

    private BehandlingRepository behandlingRepository;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;
    private PdfGenKlient pdfGen;
    private ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger;
    private BrevMottakerTjeneste brevMottakerTjeneste;
    private MicrosoftGraphTjeneste microsoftGraphTjeneste;
    private boolean enableBrevAnsvarlig;

    public VedtaksbrevGenerererTjenesteImpl() {
    }

    @Inject
    public VedtaksbrevGenerererTjenesteImpl(
        BehandlingRepository behandlingRepository, BehandlingAnsvarligRepository behandlingAnsvarligRepository,
        PdfGenKlient pdfGen,
        ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger,
        BrevMottakerTjeneste brevMottakerTjeneste,
        MicrosoftGraphTjeneste microsoftGraphTjeneste,
        @KonfigVerdi(value = "ENABLE_BREV_ANSVARLIG", defaultVerdi = "true") boolean enableBrevAnsvarlig) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
        this.pdfGen = pdfGen;
        this.manueltVedtaksbrevInnholdBygger = manueltVedtaksbrevInnholdBygger;
        this.brevMottakerTjeneste = brevMottakerTjeneste;
        this.microsoftGraphTjeneste = microsoftGraphTjeneste;
        this.enableBrevAnsvarlig = enableBrevAnsvarlig;
    }


    @WithSpan
    public GenerertBrev genererAutomatiskVedtaksbrev(VedtaksbrevGenerererInput vedtaksbrevGenerererInput) {
        Long behandlingId = vedtaksbrevGenerererInput.behandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        Vedtaksbrev vedtaksbrev = vedtaksbrevGenerererInput.vedtaksbrev();
        VedtaksbrevInnholdBygger bygger = vedtaksbrev.vedtaksbrevBygger();
        var resultat = bygger.bygg(behandling, vedtaksbrevGenerererInput.detaljertResultatTidslinje());
        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);


        var brevAnsvarlig = bestemBrevansvarlig(behandlingId);

        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.lag(
                    new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr()),
                    brevAnsvarlig,
                    behandling.getFagsakYtelseType()
                ),
                resultat.templateInnholdDto()
            )
        );

        PdfGenDokument dokument = pdfGen.lagDokument(input, vedtaksbrevGenerererInput.kunHtml());
        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            vedtaksbrev.dokumentMalType(),
            resultat.templateType()
        );
    }

    private BrevAnsvarligeDto bestemBrevansvarlig(Long behandlingId) {
        Map<BehandlingDel, BehandlingAnsvarlig> behandlingAnsvarlige = behandlingAnsvarligRepository.hentBehandlingAnsvarlige(behandlingId);

        boolean varAutomatisk = behandlingAnsvarlige.values().stream().allMatch(ba -> ba.getAnsvarligSaksbehandler() == null);

        return varAutomatisk
            ? BrevAnsvarligeDto.automatisk()
            : lagManuellBrevAnsvarlig(behandlingAnsvarlige);
    }

    private BrevAnsvarligeDto lagManuellBrevAnsvarlig(Map<BehandlingDel, BehandlingAnsvarlig> behandlingAnsvarlige) {
        if (!enableBrevAnsvarlig) {
            return new BrevAnsvarligeDto(false, List.of());
        }
        var ansvarlige = REKKEFØLGE_I_BREV.stream()
            .map(behandlingAnsvarlige::get)
            .filter(it -> it != null && it.getAnsvarligSaksbehandler() != null)
            .map(it -> new BrevAnsvarligDto(hentNavn(it.getAnsvarligSaksbehandler()), hentNavn(it.getAnsvarligBeslutter())))
            .toList();
        return new BrevAnsvarligeDto(false, ansvarlige);
    }

    private String hentNavn(String ident) {
        return Optional.ofNullable(ident)
            .flatMap(microsoftGraphTjeneste::navnPåNavAnsatt)
            .orElse(null);
    }

    @WithSpan
    @Override
    public GenerertBrev genererManuellVedtaksbrev(Long behandlingId, String brevHtml, boolean kunHtml) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        Map<BehandlingDel, BehandlingAnsvarlig> behandlingAnsvarlig = behandlingAnsvarligRepository.hentBehandlingAnsvarlige(behandlingId);
        var resultat = manueltVedtaksbrevInnholdBygger.bygg(brevHtml);
        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);

        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.lag(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr()),
                    lagManuellBrevAnsvarlig(behandlingAnsvarlig),
                    behandling.getFagsakYtelseType()
                ),
                resultat.templateInnholdDto()
            )
        );

        PdfGenDokument dokument = pdfGen.lagDokument(input, kunHtml);
        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            DokumentMalType.MANUELT_VEDTAK_DOK,
            resultat.templateType()
        );
    }
}

