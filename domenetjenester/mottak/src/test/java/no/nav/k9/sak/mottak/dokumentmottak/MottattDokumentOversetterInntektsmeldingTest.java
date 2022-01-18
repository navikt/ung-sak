package no.nav.k9.sak.mottak.dokumentmottak;

import static no.nav.k9.kodeverk.arbeidsforhold.NaturalYtelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
import static no.nav.k9.kodeverk.arbeidsforhold.NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.NaturalYtelse;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.mottak.inntektsmelding.v1.MottattDokumentOversetterInntektsmelding;
import no.nav.k9.sak.mottak.inntektsmelding.v1.MottattDokumentWrapperInntektsmelding;
import no.nav.k9.sak.mottak.inntektsmelding.xml.MottattDokumentXmlParser;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.OrgNummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MottattDokumentOversetterInntektsmeldingTest {
    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;

    @Inject
    private EntityManager entityManager;

    private VirksomhetTjeneste virksomhetTjeneste ;
    private FileToStringUtil fileToStringUtil ;
    private BehandlingRepositoryProvider repositoryProvider ;

    private InntektArbeidYtelseTjeneste iayTjeneste ;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste ;
    private MottatteDokumentRepository mottatteDokumentRepository ;
    private MottattDokumentOversetterInntektsmelding oversetter;

    @BeforeEach
    public void setUp() throws Exception {

        virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        fileToStringUtil = new FileToStringUtil();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);

        when(virksomhetTjeneste.finnOrganisasjon(ORGNR))
            .thenReturn(Optional.of(Virksomhet.getBuilder().medOrgnr(ORGNR).medNavn("Ukjent Firma").medRegistrert(LocalDate.now().minusDays(1)).build()));
        oversetter = new MottattDokumentOversetterInntektsmelding(virksomhetTjeneste);
    }

    @Test
    public void mappe_inntektsmelding_til_domene() throws IOException, URISyntaxException {
        final Behandling behandling = opprettScenarioOgLagreInntektsmelding("inntektsmelding.xml");

        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        assertThat(grunnlag).isNotNull();

        //Hent ut alle endringsrefusjoner fra alle inntektsmeldingene.
        List<Refusjon> endringerIRefusjon = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .map(i -> i.stream()
                .flatMap(im -> im.getEndringerRefusjon().stream())
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());

        assertThat(endringerIRefusjon.size()).as("Forventer at vi har en endring i refusjon lagret fra inntektsmeldingen.").isEqualTo(1);
    }

    @Test
    public void skalVedMappingLeseBeløpPerMndForNaturalytelseForGjenopptakelseFraOpphørListe() throws IOException, URISyntaxException {
        final Behandling behandling = opprettScenarioOgLagreInntektsmelding("inntektsmelding_naturalytelse_gjenopptak_ignorer_belop.xml");

        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Hent opp alle naturalytelser
        List<NaturalYtelse> naturalYtelser = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .map(e -> e.stream().flatMap(im -> im.getNaturalYtelser().stream()).collect(Collectors.toList()))
            .orElse(Collections.emptyList());

        assertThat(naturalYtelser.size()).as("Forventet fire naturalytelser, to opphørt og to gjenopptatt.").isEqualTo(4);

        assertThat(naturalYtelser.stream().map(e -> e.getType()).collect(Collectors.toList())).containsOnly(AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, ELEKTRISK_KOMMUNIKASJON);
        assertThat(naturalYtelser.stream().map(e -> e.getBeloepPerMnd())).containsOnly(new Beløp(100));
    }

    @Test
    public void skalMappeOgPersistereKorrektInnsendingsdato() throws IOException, URISyntaxException {
        // Arrange
        final Behandling behandling = opprettBehandling();
        var mottattTidspunkt = LocalDateTime.now();
        MottattDokument mottattDokument = opprettDokument(behandling, "inntektsmelding.xml", mottattTidspunkt);

        final MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getJournalpostId(),
            mottattDokument.getPayload());

        persisterInntektsmelding(behandling, mottattDokument, wrapper);

        // Assert
        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        Optional<LocalDateTime> innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream().flatMap(e -> e.stream().map(it -> it.getInnsendingstidspunkt()))
            .collect(Collectors.toList()).stream().findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValueSatisfying(it -> assertThat(it).isEqualTo(mottattTidspunkt));

    }

    @Test
    public void skalVedMottakAvNyInntektsmeldingPåSammeArbeidsforholdIkkeOverskriveHvisPersistertErNyereEnnMottatt() throws IOException, URISyntaxException {
        // Arrange
        final Behandling behandling = opprettBehandling();
        var mottattTidspunkt = LocalDateTime.now();
        MottattDokument mottattDokument = opprettDokument(behandling, "inntektsmelding.xml", mottattTidspunkt);
        MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getJournalpostId(),
            mottattDokument.getPayload());

        MottattDokumentWrapperInntektsmelding wrapperSpied = Mockito.spy(wrapper);

        LocalDateTime nyereDato = LocalDateTime.now();
        LocalDateTime eldreDato = nyereDato.minusMinutes(1);

        // Act
        // Motta nyere inntektsmelding først
        Mockito.doReturn(Optional.of(nyereDato)).when(wrapperSpied).getInnsendingstidspunkt();
        persisterInntektsmelding(behandling, mottattDokument, wrapperSpied);

        // Så motta eldre inntektsmelding
        Mockito.doReturn(Optional.of(eldreDato)).when(wrapperSpied).getInnsendingstidspunkt();
        persisterInntektsmelding(behandling, mottattDokument, wrapperSpied);

        // Assert
        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        Optional<LocalDateTime> innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream().flatMap(e -> e.stream().map(it -> it.getInnsendingstidspunkt()))
            .collect(Collectors.toList()).stream().findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValueSatisfying(it -> assertThat(it).isEqualTo(mottattTidspunkt));
        assertThat(grunnlag.getInntektsmeldinger().map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes).get()).hasSize(1);

    }

    @Test
    public void skalVedMottakAvNyInntektsmeldingPåSammeArbeidsforholdOverskriveHvisPersistertErEldreEnnMottatt() throws IOException, URISyntaxException {
        // Arrange
        final Behandling behandling = opprettBehandling();
        MottattDokument mottattDokument = opprettDokument(behandling, "inntektsmelding.xml", LocalDateTime.now());
        MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getJournalpostId(),
            mottattDokument.getPayload());

        MottattDokumentWrapperInntektsmelding wrapperSpied = Mockito.spy(wrapper);

        LocalDateTime nyereDato = LocalDateTime.now();
        LocalDateTime eldreDato = nyereDato.minusMinutes(1);

        // Act
        // Motta eldre inntektsmelding først
        Mockito.doReturn(Optional.of(eldreDato)).when(wrapperSpied).getInnsendingstidspunkt();
        mottattDokument.setKanalreferanse("AR1");
        persisterInntektsmelding(behandling, mottattDokument, wrapperSpied);

        // Så motta nyere inntektsmelding
        Mockito.doReturn(Optional.of(nyereDato)).when(wrapperSpied).getInnsendingstidspunkt();
        mottattDokument.setKanalreferanse("AR2");
        persisterInntektsmelding(behandling, mottattDokument, wrapperSpied);

        // Assert
        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        Optional<String> innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream().flatMap(e -> e.stream().map(it -> it.getKanalreferanse()))
            .collect(Collectors.toList()).stream().findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValue("AR2");
        assertThat(grunnlag.getInntektsmeldinger().map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes).get()).hasSize(1);
    }

    private void persisterInntektsmelding(final Behandling behandling, MottattDokument mottattDokument, MottattDokumentWrapperInntektsmelding wrapperSpied) {
        var innhold = oversetter.trekkUtData(wrapperSpied, mottattDokument);

        Long behandlingId = behandling.getId();
        var saksnummer = behandling.getFagsak().getSaksnummer();
        inntektsmeldingTjeneste.lagreInntektsmeldinger(saksnummer, behandlingId, List.of(innhold));
    }

    private Behandling opprettScenarioOgLagreInntektsmelding(String inntektsmeldingFilnavn) throws URISyntaxException, IOException {
        Behandling behandling = opprettBehandling();
        MottattDokument mottattDokument = opprettDokument(behandling, inntektsmeldingFilnavn, LocalDateTime.now());

        var wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getJournalpostId(), mottattDokument.getPayload());

        var innhold = oversetter.trekkUtData(wrapper, mottattDokument);

        Long behandlingId = behandling.getId();
        var saksnummer = behandling.getFagsak().getSaksnummer();
        inntektsmeldingTjeneste.lagreInntektsmeldinger(saksnummer, behandlingId, List.of(innhold));
        return behandling;
    }

    private Behandling opprettBehandling() {
        final TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        return scenario.lagre(repositoryProvider);
    }

    private MottattDokument opprettDokument(Behandling behandling, String inntektsmeldingFilnavn, LocalDateTime mottattTidspunkt) throws IOException, URISyntaxException {
        final InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
        final String xml = fileToStringUtil.readFile(inntektsmeldingFilnavn);
        final MottattDokument.Builder builder = new MottattDokument.Builder();

        MottattDokument mottattDokument = builder
            .medFagsakId(behandling.getFagsakId())
            .medMottattTidspunkt(mottattTidspunkt)
            .medMottattDato(mottattTidspunkt.toLocalDate())
            .medKanalreferanse("AR"+inntektsmeldingFilnavn)
            .medJournalPostId(new JournalpostId("123123123"))
            .medPayload(xml)
            .build();

        mottatteDokumentRepository.lagre(mottattDokument, DokumentStatus.MOTTATT);
        return mottattDokument;
    }
}
