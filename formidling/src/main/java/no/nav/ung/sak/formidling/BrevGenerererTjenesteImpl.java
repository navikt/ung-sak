package no.nav.ung.sak.formidling;


import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.EndringsInnholdBygger;
import no.nav.ung.sak.formidling.innhold.InnvilgelseInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtleder;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
public class BrevGenerererTjenesteImpl implements BrevGenerererTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BrevGenerererTjenesteImpl.class);

    private BehandlingRepository behandlingRepository;
    private AktørTjeneste aktørTjeneste;
    private PdfGenKlient pdfGen;
    private PersonopplysningRepository personopplysningRepository;
    //Gjør om til Instance<VedtaksbrevInnholdBygger> når flere maler kommer på plass, og hent vha en type
    private InnvilgelseInnholdBygger innvilgelseInnholdBygger;
    private DetaljertResultatUtleder detaljertResultatUtleder;
    private EndringsInnholdBygger endringsInnholdBygger;

    @Inject
    public BrevGenerererTjenesteImpl(
        BehandlingRepository behandlingRepository,
        AktørTjeneste aktørTjeneste,
        PdfGenKlient pdfGen,
        PersonopplysningRepository personopplysningRepository,
        InnvilgelseInnholdBygger innvilgelseInnholdBygger,
        DetaljertResultatUtleder detaljertResultatUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.aktørTjeneste = aktørTjeneste;
        this.pdfGen = pdfGen;
        this.personopplysningRepository = personopplysningRepository;
        this.innvilgelseInnholdBygger = innvilgelseInnholdBygger;
        this.detaljertResultatUtleder = detaljertResultatUtleder;
    }

    public BrevGenerererTjenesteImpl() {
    }

    @WithSpan
    @Override
    public GenerertBrev genererVedtaksbrev(Long behandlingId) {
        return BrevGenereringSemafor.begrensetParallellitet( () -> doGenererVedtaksbrev(behandlingId));
    }

    @WithSpan //WithSpan her for å kunne skille ventetid på semafor i opentelemetry
    private GenerertBrev doGenererVedtaksbrev(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (!behandling.erAvsluttet()) {
            throw new IllegalStateException("Behandling må være avsluttet for å kunne bestille vedtaksbrev");
        }

        var bygger = bestemBygger(behandling);

        var resultat = bygger.bygg(behandling);
        var pdlMottaker = hentMottaker(behandling);
        var input = new TemplateInput(resultat.templateType(),
            new TemplateDto(
                FellesDto.automatisk(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr())),
                resultat.templateInnholdDto()
            )
        );
        PdfGenDokument dokument = pdfGen.lagDokument(input);
        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            resultat.dokumentMalType(),
            resultat.templateType()
        );
    }

    private VedtaksbrevInnholdBygger bestemBygger(Behandling behandling) {
        var resultater = detaljertResultatUtleder.utledDetaljertResultat(behandling)
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatTyper().stream())
            .collect(Collectors.toSet());

        if (resultater.equals(Collections.singleton(DetaljertResultatType.INNVILGET_NY_PERIODE))) {
            return innvilgelseInnholdBygger;
        } else if (resultater.equals(Collections.singleton(DetaljertResultatType.ENDRING_RAPPORTERT_INNTEKT))){
            return endringsInnholdBygger;
        } else {
            throw new IllegalStateException("Støtter ikke vedtaksbrev for avslag foreløpig. BehandlingResultat=" + behandling.getBehandlingResultatType());
        }

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

