package no.nav.ung.sak.formidling.vedtak;


import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.BrevGenereringSemafor;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class VedtaksbrevGenerererTjenesteImpl implements VedtaksbrevGenerererTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevGenerererTjenesteImpl.class);

    private BehandlingRepository behandlingRepository;
    private PdfGenKlient pdfGen;
    private ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger;
    private BrevMottakerTjeneste brevMottakerTjeneste;

    @Inject
    public VedtaksbrevGenerererTjenesteImpl(
        BehandlingRepository behandlingRepository,
        PdfGenKlient pdfGen,
        ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger, BrevMottakerTjeneste brevMottakerTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.pdfGen = pdfGen;
        this.manueltVedtaksbrevInnholdBygger = manueltVedtaksbrevInnholdBygger;
        this.brevMottakerTjeneste = brevMottakerTjeneste;
    }

    public VedtaksbrevGenerererTjenesteImpl() {
    }

    @WithSpan
    @Override
    public GenerertBrev genererAutomatiskVedtaksbrev(VedtaksbrevGenerererInput vedtaksbrevGenerererInput) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererAutomatiskVedtaksbrev(vedtaksbrevGenerererInput));
    }

    @WithSpan
    private GenerertBrev doGenererAutomatiskVedtaksbrev(VedtaksbrevGenerererInput vedtaksbrevGenerererInput) {
        var behandling = behandlingRepository.hentBehandling(vedtaksbrevGenerererInput.behandlingId());

        Vedtaksbrev vedtaksbrev = vedtaksbrevGenerererInput.vedtaksbrev();
        VedtaksbrevInnholdBygger bygger = vedtaksbrev.vedtaksbrevBygger();
        var resultat = bygger.bygg(behandling, vedtaksbrevGenerererInput.detaljertResultatTidslinje());
        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);
        var automatiskGenerertFooter = harManuellAksjonspunkt(behandling);
        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.lag(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr()), automatiskGenerertFooter),
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

    private static boolean harManuellAksjonspunkt(Behandling behandling) {
        return behandling.getAksjonspunkter().stream().anyMatch(it -> !it.getAksjonspunktDefinisjon().getAksjonspunktType().erAutopunkt() && it.erUtført());
    }


    /**
     * Lager manuell brev lagret i databasen
     */
    @WithSpan
    @Override
    public GenerertBrev genererManuellVedtaksbrev(Long behandlingId, String brevHtml, boolean kunHtml) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererManuellVedtaksbrev(behandlingId, brevHtml, kunHtml));
    }

    @WithSpan
    private GenerertBrev doGenererManuellVedtaksbrev(Long behandlingId, String brevHtml, boolean kunHtml) {
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

