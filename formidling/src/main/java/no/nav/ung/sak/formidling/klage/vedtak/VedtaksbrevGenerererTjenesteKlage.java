package no.nav.ung.sak.formidling.klage.vedtak;


import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.klage.regler.BehandlingVedtaksbrevResultatKlage;
import no.nav.ung.sak.formidling.klage.regler.VedtaksbrevReglerKlage;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererInput;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererTjeneste;

@ApplicationScoped
@BehandlingTypeRef(BehandlingType.KLAGE)
@FagsakYtelseTypeRef
public class VedtaksbrevGenerererTjenesteKlage implements VedtaksbrevGenerererTjeneste {

    private BehandlingRepository behandlingRepository;
    private PdfGenKlient pdfGen;
    private ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger;
    private BrevMottakerTjeneste brevMottakerTjeneste;
    private VedtaksbrevReglerKlage vedtaksbrevRegler;

    public VedtaksbrevGenerererTjenesteKlage() {
    }

    @Inject
    public VedtaksbrevGenerererTjenesteKlage(
        BehandlingRepository behandlingRepository,
        PdfGenKlient pdfGen,
        ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger, BrevMottakerTjeneste brevMottakerTjeneste,
        @Any VedtaksbrevReglerKlage vedtaksbrevRegler) {
        this.behandlingRepository = behandlingRepository;
        this.pdfGen = pdfGen;
        this.manueltVedtaksbrevInnholdBygger = manueltVedtaksbrevInnholdBygger;
        this.brevMottakerTjeneste = brevMottakerTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
    }


    @Override
    public GenerertBrev genererAutomatiskVedtaksbrev(Behandling behandling, DokumentMalType dokumentMalType, boolean kunHtml) {
        BehandlingVedtaksbrevResultatKlage totalresultater = vedtaksbrevRegler.kjør(behandling.getId());

        var vedtaksbrev = totalresultater.vedtaksbrevResultater().stream()
            .filter(it -> it.dokumentMalType() == dokumentMalType)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("DokumentmalType " + dokumentMalType + " er ikke gyldig. Resultat fra regler: " + totalresultater.safePrint()));

        return genererAutomatiskVedtaksbrev(
            new VedtaksbrevGenerererInput(behandling.getId(), vedtaksbrev, null, false)
        );
    }

    @WithSpan
    private GenerertBrev genererAutomatiskVedtaksbrev(VedtaksbrevGenerererInput vedtaksbrevGenerererInput) {
        var behandling = behandlingRepository.hentBehandling(vedtaksbrevGenerererInput.behandlingId());

        var vedtaksbrev = vedtaksbrevGenerererInput.vedtaksbrev();
        var bygger = vedtaksbrev.vedtaksbrevBygger();
        var resultat = bygger.bygg(behandling);
        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);
        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.lag(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr()), resultat.automatiskGenerertFooter()),
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

    @WithSpan
    @Override
    public GenerertBrev genererManuellVedtaksbrev(Long behandlingId, String brevHtml, boolean kunHtml) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var resultat = manueltVedtaksbrevInnholdBygger.bygg(brevHtml);
        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);
        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.lag(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr()), false),
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

