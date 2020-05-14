package no.nav.k9.sak.mottak.dokumentmottak;

import static no.nav.k9.kodeverk.arbeidsforhold.NaturalYtelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
import static no.nav.k9.kodeverk.arbeidsforhold.NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
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
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.OrganisasjonsDetaljer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.UstrukturertNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonConsumer;

public class MottattDokumentOversetterInntektsmeldingTest {
    private static final DatatypeFactory DATATYPE_FACTORY;
    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private final OrganisasjonConsumer organisasjonConsumer = mock(OrganisasjonConsumer.class);
    private final FileToStringUtil fileToStringUtil = new FileToStringUtil();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private MottatteDokumentRepository mottatteDokumentRepository = new MottatteDokumentRepository(repoRule.getEntityManager());
    private MottattDokumentOversetterInntektsmelding oversetter;

    @Before
    public void setUp() throws Exception {
        final HentOrganisasjonResponse hentOrganisasjonResponse = new HentOrganisasjonResponse();
        final Organisasjon value = new Organisasjon();
        final UstrukturertNavn navn = new UstrukturertNavn();
        navn.getNavnelinje().add("Color Line");
        value.setNavn(navn);
        value.setOrgnummer("119999996");
        final OrganisasjonsDetaljer detaljer = new OrganisasjonsDetaljer();
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        detaljer.setRegistreringsDato(DATATYPE_FACTORY.newXMLGregorianCalendar(c));
        value.setOrganisasjonDetaljer(detaljer);
        hentOrganisasjonResponse.setOrganisasjon(value);
        when(organisasjonConsumer.hentOrganisasjon(any())).thenReturn(hentOrganisasjonResponse);
        VirksomhetTjeneste virksomhetTjeneste = new VirksomhetTjeneste(organisasjonConsumer, new VirksomhetRepository());

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
        MottattDokument mottattDokument = opprettDokument(behandling, "inntektsmelding.xml");

        final MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getPayload());

        persisterInntektsmelding(behandling, mottattDokument, wrapper);

        // Assert
        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        Optional<LocalDateTime> innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream().flatMap(e -> e.stream().map(it -> it.getInnsendingstidspunkt()))
            .collect(Collectors.toList()).stream().findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValue(wrapper.getInnsendingstidspunkt().get());

    }

    @Test
    public void skalVedMottakAvNyInntektsmeldingPåSammeArbeidsforholdIkkeOverskriveHvisPersistertErNyereEnnMottatt() throws IOException, URISyntaxException {
        // Arrange
        final Behandling behandling = opprettBehandling();
        MottattDokument mottattDokument = opprettDokument(behandling, "inntektsmelding.xml");
        MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getPayload());

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
        assertThat(innsendingstidspunkt).hasValue(nyereDato);
        assertThat(grunnlag.getInntektsmeldinger().map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes).get()).hasSize(1);

    }

    @Test
    public void skalVedMottakAvNyInntektsmeldingPåSammeArbeidsforholdOverskriveHvisPersistertErEldreEnnMottatt() throws IOException, URISyntaxException {
        // Arrange
        final Behandling behandling = opprettBehandling();
        MottattDokument mottattDokument = opprettDokument(behandling, "inntektsmelding.xml");
        MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getPayload());

        MottattDokumentWrapperInntektsmelding wrapperSpied = Mockito.spy(wrapper);

        LocalDateTime nyereDato = LocalDateTime.now();
        LocalDateTime eldreDato = nyereDato.minusMinutes(1);

        // Act
        // Motta eldre inntektsmelding først
        Mockito.doReturn(Optional.of(eldreDato)).when(wrapperSpied).getInnsendingstidspunkt();
        persisterInntektsmelding(behandling, mottattDokument, wrapperSpied);

        // Så motta nyere inntektsmelding
        Mockito.doReturn(Optional.of(nyereDato)).when(wrapperSpied).getInnsendingstidspunkt();
        persisterInntektsmelding(behandling, mottattDokument, wrapperSpied);

        // Assert
        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        Optional<LocalDateTime> innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream().flatMap(e -> e.stream().map(it -> it.getInnsendingstidspunkt()))
            .collect(Collectors.toList()).stream().findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValue(nyereDato);
        assertThat(grunnlag.getInntektsmeldinger().map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes).get()).hasSize(1);
    }

    private void persisterInntektsmelding(final Behandling behandling, MottattDokument mottattDokument, MottattDokumentWrapperInntektsmelding wrapperSpied) {
        var innhold = oversetter.trekkUtData(wrapperSpied, mottattDokument, behandling);
        
        Long behandlingId = behandling.getId();
        var saksnummer = behandling.getFagsak().getSaksnummer();
        inntektsmeldingTjeneste.lagreInntektsmeldinger(saksnummer, behandlingId, List.of(innhold));
    }

    private Behandling opprettScenarioOgLagreInntektsmelding(String inntektsmeldingFilnavn) throws URISyntaxException, IOException {
        Behandling behandling = opprettBehandling();
        MottattDokument mottattDokument = opprettDokument(behandling, inntektsmeldingFilnavn);

        var wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getPayload());

        var innhold = oversetter.trekkUtData(wrapper, mottattDokument, behandling);
        
        Long behandlingId = behandling.getId();
        var saksnummer = behandling.getFagsak().getSaksnummer();
        inntektsmeldingTjeneste.lagreInntektsmeldinger(saksnummer, behandlingId, List.of(innhold));
        return behandling;
    }

    private Behandling opprettBehandling() {
        final TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        return scenario.lagre(repositoryProvider);
    }

    private MottattDokument opprettDokument(Behandling behandling, String inntektsmeldingFilnavn) throws IOException, URISyntaxException {
        final InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
        final String xml = fileToStringUtil.readFile(inntektsmeldingFilnavn);
        final MottattDokument.Builder builder = new MottattDokument.Builder();

        MottattDokument mottattDokument = builder
            .medFagsakId(behandling.getFagsakId())
            .medMottattDato(LocalDate.now())
            .medJournalPostId(new JournalpostId("123123123"))
            .medPayload(xml)
            .build();

        mottatteDokumentRepository.lagre(mottattDokument);
        return mottattDokument;
    }
}
