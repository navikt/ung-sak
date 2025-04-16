package no.nav.ung.sak.formidling;


import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
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

    private VedtaksbrevRegler vedtaksbrevRegler;

    @Inject
    public BrevGenerererTjenesteImpl(
            BehandlingRepository behandlingRepository,
            AktørTjeneste aktørTjeneste,
            PdfGenKlient pdfGen,
            PersonopplysningRepository personopplysningRepository,
            VedtaksbrevRegler vedtaksbrevRegler) {

        this.behandlingRepository = behandlingRepository;
        this.aktørTjeneste = aktørTjeneste;
        this.pdfGen = pdfGen;
        this.personopplysningRepository = personopplysningRepository;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
    }

    public BrevGenerererTjenesteImpl() {
    }

    @WithSpan
    @Override
    public GenerertBrev genererVedtaksbrev(Long behandlingId) {
        return BrevGenereringSemafor.begrensetParallellitet( () -> doGenererVedtaksbrev(behandlingId, false));
    }

    @WithSpan
    @Override
    public GenerertBrev genererVedtaksbrevKunHtml(Long behandlingId) {
        return doGenererVedtaksbrev(behandlingId, true);
    }

    @WithSpan //WithSpan her for å kunne skille ventetid på semafor i opentelemetry
    private GenerertBrev doGenererVedtaksbrev(Long behandlingId, boolean kunHtml) {
        VedtaksbrevRegelResulat regelResultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("Resultat fra vedtaksbrev regler: {}", regelResultat.safePrint());

        if (!regelResultat.vedtaksbrevEgenskaper().harBrev()) {
            LOG.warn(regelResultat.forklaring());
            return null;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        VedtaksbrevInnholdBygger bygger = regelResultat.bygger();
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

