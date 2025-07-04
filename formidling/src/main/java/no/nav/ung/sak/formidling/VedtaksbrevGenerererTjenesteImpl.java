package no.nav.ung.sak.formidling;


import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.ung.sak.formidling.vedtak.DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING;

@ApplicationScoped
public class VedtaksbrevGenerererTjenesteImpl implements VedtaksbrevGenerererTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevGenerererTjenesteImpl.class);

    private BehandlingRepository behandlingRepository;
    private PdfGenKlient pdfGen;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;
    private ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger;
    private BrevMottakerTjeneste brevMottakerTjeneste;

    private VedtaksbrevRegler vedtaksbrevRegler;

    @Inject
    public VedtaksbrevGenerererTjenesteImpl(
        BehandlingRepository behandlingRepository,
        PdfGenKlient pdfGen,
        VedtaksbrevRegler vedtaksbrevRegler,
        VedtaksbrevValgRepository vedtaksbrevValgRepository,
        ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger, BrevMottakerTjeneste brevMottakerTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.pdfGen = pdfGen;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.manueltVedtaksbrevInnholdBygger = manueltVedtaksbrevInnholdBygger;
        this.brevMottakerTjeneste = brevMottakerTjeneste;
    }

    public VedtaksbrevGenerererTjenesteImpl() {
    }

    /**
     * Lager brev for behandling basert på valg gjort av saksbehandler
     */
    @WithSpan
    @Override
    public GenerertBrev genererVedtaksbrevForBehandling(Long behandlingId, boolean kunHtml) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererVedtaksbrev(behandlingId, kunHtml));
    }

    @WithSpan //WithSpan her for å kunne skille ventetid på semafor i opentelemetry
    private GenerertBrev doGenererVedtaksbrev(Long behandlingId, boolean kunHtml) {
        VedtaksbrevValgEntitet vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId).orElse(null);
        if (vedtaksbrevValgEntitet != null) {
            if (vedtaksbrevValgEntitet.isHindret()) {
                LOG.info("Vedtaksbrev er manuelt stoppet - lager ikke brev");
                return null;
            }
            if (vedtaksbrevValgEntitet.isRedigert()) {
                LOG.info("Vedtaksbrev er manuelt redigert - genererer manuell brev");
                return doGenererManuellVedtaksbrev(behandlingId, kunHtml);
            }
            LOG.warn("Vedtaksbrevvalg lagret, men verken hindret eller redigert");
        }

        return doGenererAutomatiskVedtaksbrev(behandlingId, kunHtml);
    }

    /**
     * Lager brev basert på regler
     */
    @WithSpan
    @Override
    public GenerertBrev genererAutomatiskVedtaksbrev(Long behandlingId, boolean kunHtml) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererAutomatiskVedtaksbrev(behandlingId, kunHtml));
    }

    @WithSpan
    private GenerertBrev doGenererAutomatiskVedtaksbrev(Long behandlingId, boolean kunHtml) {
        VedtaksbrevRegelResulat regelResultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("Resultat fra vedtaksbrev regler: {}", regelResultat.safePrint());

        if (!regelResultat.vedtaksbrevEgenskaper().harBrev()) {
            if (erIngenBrevForventet(regelResultat)) {
                LOG.info(regelResultat.forklaring());
            } else {
                LOG.warn(regelResultat.forklaring());
            }
            return null;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        VedtaksbrevInnholdBygger bygger = regelResultat.automatiskVedtaksbrevBygger();
        var resultat = bygger.bygg(behandling, regelResultat.detaljertResultatTimeline());
        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);
        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.automatisk(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr())),
                resultat.templateInnholdDto()
            )
        );

        PdfGenDokument dokument = pdfGen.lagDokument(input, kunHtml);
        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            resultat.dokumentMalType(),
            resultat.templateType()
        );
    }

    private static boolean erIngenBrevForventet(VedtaksbrevRegelResulat regelResultat) {
        var harKunFullUtbetalingEtterKontroll = regelResultat.detaljertResultatTimeline().toSegments().stream()
            .allMatch(segment ->
                segment.getValue().resultatInfo().stream().map(DetaljertResultatInfo::detaljertResultatType)
                    .allMatch(KONTROLLER_INNTEKT_FULL_UTBETALING::equals));
        return harKunFullUtbetalingEtterKontroll;
    }

    /**
     * Lager manuell brev lagret i databasen uten å kjøre brevregler
     */
    @WithSpan
    @Override
    public GenerertBrev genererManuellVedtaksbrev(Long behandlingId, boolean kunHtml) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererManuellVedtaksbrev(behandlingId, kunHtml));
    }

    @WithSpan
    private GenerertBrev doGenererManuellVedtaksbrev(Long behandlingId, boolean kunHtml) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var resultat = manueltVedtaksbrevInnholdBygger.bygg(behandling, null);
        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);
        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.manuell(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr())),
                resultat.templateInnholdDto()
            )
        );

        PdfGenDokument dokument = pdfGen.lagDokument(input, kunHtml);
        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            resultat.dokumentMalType(),
            resultat.templateType()
        );
    }


}

