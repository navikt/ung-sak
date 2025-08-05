package no.nav.ung.sak.formidling.vedtak;


import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
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
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrev;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class VedtaksbrevGenerererTjenesteImpl implements VedtaksbrevGenerererTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevGenerererTjenesteImpl.class);

    private BehandlingRepository behandlingRepository;
    private PdfGenKlient pdfGen;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;
    private ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger;
    private BrevMottakerTjeneste brevMottakerTjeneste;
    private boolean enableIgnoreManglendeBrev;

    private VedtaksbrevRegler vedtaksbrevRegler;

    @Inject
    public VedtaksbrevGenerererTjenesteImpl(
        BehandlingRepository behandlingRepository,
        PdfGenKlient pdfGen,
        VedtaksbrevRegler vedtaksbrevRegler,
        VedtaksbrevValgRepository vedtaksbrevValgRepository,
        ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger, BrevMottakerTjeneste brevMottakerTjeneste,
        @KonfigVerdi(value = "IGNORE_MANGLENDE_BREV", defaultVerdi = "false") boolean ignoreManglendeBrev) {

        this.behandlingRepository = behandlingRepository;
        this.pdfGen = pdfGen;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.manueltVedtaksbrevInnholdBygger = manueltVedtaksbrevInnholdBygger;
        this.brevMottakerTjeneste = brevMottakerTjeneste;
        this.enableIgnoreManglendeBrev = ignoreManglendeBrev;
    }

    public VedtaksbrevGenerererTjenesteImpl() {
    }

    /**
     * Lager brev for behandling basert på valg gjort av saksbehandler
     */
    @WithSpan
    @Override
    public GenerertBrev genererVedtaksbrevForBehandling(VedtaksbrevBestillingInput vedtaksbrevBestillingInput) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererVedtaksbrev(vedtaksbrevBestillingInput));
    }

    //TODO Flytt denne til tjeneste/vurderer task
    @WithSpan //WithSpan her for å kunne skille ventetid på semafor i opentelemetry
    private GenerertBrev doGenererVedtaksbrev(VedtaksbrevBestillingInput vedtaksbrevBestillingInput) {
        VedtaksbrevValgEntitet vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(vedtaksbrevBestillingInput.behandlingId()).orElse(null);
        if (vedtaksbrevValgEntitet != null) {
            if (vedtaksbrevValgEntitet.isHindret()) {
                LOG.info("Vedtaksbrev er manuelt stoppet - lager ikke brev");
                return null;
            }
            if (vedtaksbrevValgEntitet.isRedigert()) {
                LOG.info("Vedtaksbrev er manuelt redigert - genererer manuell brev");
                return doGenererManuellVedtaksbrev(vedtaksbrevBestillingInput);
            }
            LOG.warn("Vedtaksbrevvalg lagret, men verken hindret eller redigert");
        }

        return doGenererAutomatiskVedtaksbrev(vedtaksbrevBestillingInput);
    }

    /**
     * Lager brev basert på regler
     */
    @WithSpan
    @Override
    public GenerertBrev genererAutomatiskVedtaksbrev(VedtaksbrevBestillingInput vedtaksbrevBestillingInput) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererAutomatiskVedtaksbrev(vedtaksbrevBestillingInput));
    }

    @WithSpan
    private GenerertBrev doGenererAutomatiskVedtaksbrev(VedtaksbrevBestillingInput vedtaksbrevBestillingInput) {
        BehandlingVedtaksbrevResultat regelResultater = vedtaksbrevRegler.kjør(vedtaksbrevBestillingInput.behandlingId());
        LOG.info("Resultat fra vedtaksbrev regler: {}", regelResultater.safePrint());

        if (!regelResultater.harBrev()) {
            håndterIngenBrevResultat(regelResultater.ingenBrevResultater().getFirst());
            return null;
        }

        var behandling = behandlingRepository.hentBehandling(vedtaksbrevBestillingInput.behandlingId());

        VedtaksbrevInnholdBygger bygger = regelResultater.vedtaksbrevResultater().getFirst().vedtaksbrevBygger();
        var resultat = bygger.bygg(behandling, regelResultater.detaljertResultatTimeline());
        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);
        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.automatisk(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr())),
                resultat.templateInnholdDto()
            )
        );

        PdfGenDokument dokument = pdfGen.lagDokument(input, vedtaksbrevBestillingInput.kunHtml());
        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            resultat.dokumentMalType(),
            resultat.templateType()
        );
    }

    private void håndterIngenBrevResultat(IngenBrev ingenBrevResultat) {
        if (ingenBrevResultat.ingenBrevÅrsakType() == IngenBrevÅrsakType.IKKE_IMPLEMENTERT) {
            if (enableIgnoreManglendeBrev) {
                LOG.warn("Ingen brev implementert for tilfelle : {}", ingenBrevResultat.forklaring());
            }
            else {
                throw new IllegalStateException("Feiler pga ingen brev implementert for tilfelle: " + ingenBrevResultat.forklaring());
            }
        }
        LOG.info("Ingen brev relevant for tilfelle: {}", ingenBrevResultat.forklaring());
    }

    /**
     * Lager manuell brev lagret i databasen uten å kjøre brevregler
     */
    @WithSpan
    @Override
    public GenerertBrev genererManuellVedtaksbrev(VedtaksbrevBestillingInput vedtaksbrevBestillingInput) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererManuellVedtaksbrev(vedtaksbrevBestillingInput));
    }

    @WithSpan
    private GenerertBrev doGenererManuellVedtaksbrev(VedtaksbrevBestillingInput vedtaksbrevBestillingInput) {
        var behandling = behandlingRepository.hentBehandling(vedtaksbrevBestillingInput.behandlingId());
        var resultat = manueltVedtaksbrevInnholdBygger.bygg(behandling, null);
        var pdlMottaker = brevMottakerTjeneste.hentMottaker(behandling);
        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.manuell(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr())),
                resultat.templateInnholdDto()
            )
        );

        PdfGenDokument dokument = pdfGen.lagDokument(input, vedtaksbrevBestillingInput.kunHtml());
        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            resultat.dokumentMalType(),
            resultat.templateType()
        );
    }


}

