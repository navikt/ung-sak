package no.nav.ung.sak.formidling;


import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.ManuellVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@ApplicationScoped
public class BrevGenerererTjenesteImpl implements BrevGenerererTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BrevGenerererTjenesteImpl.class);

    private BehandlingRepository behandlingRepository;
    private AktørTjeneste aktørTjeneste;
    private PdfGenKlient pdfGen;
    private PersonopplysningRepository personopplysningRepository;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;
    private ManuellVedtaksbrevInnholdBygger manuellVedtaksbrevInnholdBygger;

    private VedtaksbrevRegler vedtaksbrevRegler;

    @Inject
    public BrevGenerererTjenesteImpl(
        BehandlingRepository behandlingRepository,
        AktørTjeneste aktørTjeneste,
        PdfGenKlient pdfGen,
        PersonopplysningRepository personopplysningRepository,
        VedtaksbrevRegler vedtaksbrevRegler,
        VedtaksbrevValgRepository vedtaksbrevValgRepository,
        ManuellVedtaksbrevInnholdBygger manuellVedtaksbrevInnholdBygger) {

        this.behandlingRepository = behandlingRepository;
        this.aktørTjeneste = aktørTjeneste;
        this.pdfGen = pdfGen;
        this.personopplysningRepository = personopplysningRepository;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.manuellVedtaksbrevInnholdBygger = manuellVedtaksbrevInnholdBygger;
    }

    public BrevGenerererTjenesteImpl() {
    }

    @WithSpan
    @Override
    public GenerertBrev genererVedtaksbrevForBehandling(Long behandlingId, boolean kunHtml) {
        return BrevGenereringSemafor.begrensetParallellitet( () -> doGenererVedtaksbrev(behandlingId, kunHtml));
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
    public GenerertBrev genererAutomatiskVedtaksbrev(Long behandlingId, boolean kunHtml) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererAutomatiskVedtaksbrev(behandlingId, kunHtml));
    }

    @WithSpan
    private GenerertBrev doGenererAutomatiskVedtaksbrev(Long behandlingId, boolean kunHtml) {
        VedtaksbrevRegelResulat regelResultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("Resultat fra vedtaksbrev regler: {}", regelResultat.safePrint());

        if (!regelResultat.vedtaksbrevEgenskaper().harBrev()) {
            LOG.warn(regelResultat.forklaring());
            return null;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        VedtaksbrevInnholdBygger bygger = regelResultat.automatiskVedtaksbrevBygger();
        var resultat = bygger.bygg(behandling, regelResultat.detaljertResultatTimeline());
        var pdlMottaker = hentMottaker(behandling);
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

    @WithSpan
    public GenerertBrev genererManuellVedtaksbrev(Long behandlingId, boolean kunHtml) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doGenererManuellVedtaksbrev(behandlingId, kunHtml));
    }

    @WithSpan
    private GenerertBrev doGenererManuellVedtaksbrev(Long behandlingId, boolean kunHtml) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var resultat = manuellVedtaksbrevInnholdBygger.bygg(behandling, null);
        var pdlMottaker = hentMottaker(behandling);
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

    private PdlPerson hentMottaker(Behandling behandling) {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlagEntitet = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        PersonopplysningEntitet personopplysning = personopplysningGrunnlagEntitet.getGjeldendeVersjon().getPersonopplysning(behandling.getAktørId());

        String navn = personopplysning.getNavn();

        AktørId aktørId = behandling.getFagsak().getAktørId();
        var personIdent = aktørTjeneste.hentPersonIdentForAktørId(aktørId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke person med aktørid"));

        String fnr = personIdent.getIdent();
        Objects.requireNonNull(fnr);

        return new PdlPerson(fnr, aktørId, navn);
    }


}

