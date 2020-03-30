package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsmeldingInnsendingsårsak;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class VurderArbeidsforholdTjenesteImplTest {

    private static final LocalDate IDAG = LocalDate.now();
    private LocalDateTime nåTid = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private volatile int nåTidTeller;

    private final LocalDate skjæringstidspunkt = IDAG.minusDays(30);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private IAYRepositoryProvider repositoryProvider = new IAYRepositoryProvider(repositoryRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private InntektsmeldingFilterYtelse inntektsmeldingFilterYtelse = new InntektsmeldingFilterYtelseImpl();
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste = new InntektsmeldingRegisterTjeneste(iayTjeneste, inntektsmeldingTjeneste, null, new UnitTestLookupInstanceImpl<>(inntektsmeldingFilterYtelse));
    private PåkrevdeInntektsmeldingerTjeneste påkrevdeInntektsmeldingerTjeneste = new PåkrevdeInntektsmeldingerTjeneste(inntektsmeldingArkivTjeneste, repositoryProvider.getSøknadRepository());
    private VurderArbeidsforholdTjeneste tjeneste = new VurderArbeidsforholdTjeneste(iayTjeneste, påkrevdeInntektsmeldingerTjeneste);

    @Before
    public void setup(){
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt() {
        final var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);

        final Behandling behandling = scenario.lagre(repositoryProvider);

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final YrkesaktivitetBuilder yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        final AktivitetsAvtaleBuilder avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        final AktivitetsAvtaleBuilder avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        sendNyInntektsmelding(behandling, virksomhet, ref);

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();

        avsluttBehandlingOgFagsak(behandling);
        
        @SuppressWarnings("unused")
        var revurdering = opprettRevurderingsbehandling(behandling);

        sendInnInntektsmelding(behandling, virksomhet, null);

        vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_2() {
        final var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);

        final Behandling behandling = scenario.lagre(repositoryProvider);

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final YrkesaktivitetBuilder yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        final AktivitetsAvtaleBuilder avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        final AktivitetsAvtaleBuilder avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder)
            .leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendNyInntektsmelding(behandling, virksomhet, ref);

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();

        avsluttBehandlingOgFagsak(behandling);
        
        @SuppressWarnings("unused")
        var revurdering = opprettRevurderingsbehandling(behandling);

        sendInnInntektsmelding(behandling, virksomhet, ref);

        vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> hentArbeidsforhold(final Behandling behandling) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(behandling.getFagsak().getSaksnummer());
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = tjeneste.vurder(lagRef(behandling), iayGrunnlag, sakInntektsmeldinger, true);
        return vurder;
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_3() {
        final var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        final Behandling behandling = scenario.lagre(repositoryProvider);

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final String orgnummer = "123123123";
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet(orgnummer));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);

        final YrkesaktivitetBuilder yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef, virksomhet), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        final AktivitetsAvtaleBuilder avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        final AktivitetsAvtaleBuilder avtaleBuilder3 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder3.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder3);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        var ref1 = EksternArbeidsforholdRef.ref("ref1");
        var internRef1 = builder.medNyInternArbeidsforholdRef(virksomhet, ref1);
        final YrkesaktivitetBuilder yrkesBuilder1 = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef1, virksomhet), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder1.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef1);
        final AktivitetsAvtaleBuilder avtaleBuilder1 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        final AktivitetsAvtaleBuilder avtaleBuilder2 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder1.leggTilAktivitetsAvtale(avtaleBuilder1).leggTilAktivitetsAvtale(avtaleBuilder2);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder1);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendNyInntektsmelding(behandling, virksomhet, ref);
        sendNyInntektsmelding(behandling, virksomhet, ref1);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(behandling.getFagsak().getSaksnummer());
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = tjeneste.vurder(lagRef(behandling), iayGrunnlag, sakInntektsmeldinger, false);
        assertThat(vurder).isEmpty();
    }

    @Test
    public void skal_ikke_gi_autopunkt_når_inntektsmelding_og_aareg_matcher() {
        //Arrange
        final var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);

        final Behandling behandling = scenario.lagre(repositoryProvider);
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        opprettAktørArbeidMedYrkesaktivitet(behandling, ref, virksomhet);
        sendNyInntektsmelding(behandling, virksomhet, ref);

        //Act
        boolean resultat = tjeneste.inntektsmeldingMedArbeidsforholdIdSomIkkeMatcherArbeidsforholdIAAReg(behandling.getId(), behandling.getAktørId(), skjæringstidspunkt);
        //Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_ikke_gi_autopunkt_når_inntektsmelding_uten_arbeidsforholdId() {
        //Arrange
        final var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        final Behandling behandling = scenario.lagre(repositoryProvider);
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        opprettAktørArbeidMedYrkesaktivitet(behandling, ref, virksomhet);
        sendNyInntektsmelding(behandling, virksomhet, null);

        //Act
        boolean resultat = tjeneste.inntektsmeldingMedArbeidsforholdIdSomIkkeMatcherArbeidsforholdIAAReg(behandling.getId(), behandling.getAktørId(), skjæringstidspunkt);
        //Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_ikke_gi_autopunkt_når_arbeidforholdId_mangler_fra_aareg() {
        //Arrange
        final var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        final Behandling behandling = scenario.lagre(repositoryProvider);
        var ref = EksternArbeidsforholdRef.ref("ref");
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        opprettAktørArbeidMedYrkesaktivitet(behandling, null, virksomhet);
        sendNyInntektsmelding(behandling, virksomhet, ref);

        //Act
        boolean resultat = tjeneste.inntektsmeldingMedArbeidsforholdIdSomIkkeMatcherArbeidsforholdIAAReg(behandling.getId(), behandling.getAktørId(), skjæringstidspunkt);
        //Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_og_deretter_ikke__gi_autopunkt_når_arbeidsforholdId_i_inntektsmelding_ikke_matcher_i_aareg_og_deretter_ny_gyldig_inntektsmelding_uten_arbId() {
        //Arrange
        final var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        final Behandling behandling = scenario.lagre(repositoryProvider);
        var ref = EksternArbeidsforholdRef.ref("ref");
        var ukjentRef = EksternArbeidsforholdRef.ref("ukjentRef");
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        opprettAktørArbeidMedYrkesaktivitet(behandling, ref, virksomhet);
        sendNyInntektsmelding(behandling, virksomhet, ukjentRef);
        //Act
        boolean resultat = tjeneste.inntektsmeldingMedArbeidsforholdIdSomIkkeMatcherArbeidsforholdIAAReg(behandling.getId(), behandling.getAktørId(), skjæringstidspunkt);
        //Assert
        assertThat(resultat).isTrue();
        //Arrange
        sendNyInntektsmelding(behandling, virksomhet, null);
        //Act
        boolean resultat2 = tjeneste.inntektsmeldingMedArbeidsforholdIdSomIkkeMatcherArbeidsforholdIAAReg(behandling.getId(), behandling.getAktørId(), skjæringstidspunkt);
        //Assert
        assertThat(resultat2).isFalse();
    }

    @Test
    public void skal_gi_og_deretter_ikke__gi_autopunkt_når_arbeidsforholdId_i_inntektsmelding_ikke_matcher_i_aareg_og_deretter_ny_gyldig_inntektsmelding() {
        //Arrange
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);
        var ref = EksternArbeidsforholdRef.ref("ref");
        var ukjentRef = EksternArbeidsforholdRef.ref("ukjentRef");
        var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        opprettAktørArbeidMedYrkesaktivitet(behandling, ref, virksomhet);
        sendNyInntektsmelding(behandling, virksomhet, ukjentRef);
        //Act
        boolean resultat = tjeneste.inntektsmeldingMedArbeidsforholdIdSomIkkeMatcherArbeidsforholdIAAReg(behandling.getId(), behandling.getAktørId(), skjæringstidspunkt);
        //Assert
        assertThat(resultat).isTrue();
        //Arrange
        sendNyInntektsmelding(behandling, virksomhet, ref);
        //Act
        boolean resultat2 = tjeneste.inntektsmeldingMedArbeidsforholdIdSomIkkeMatcherArbeidsforholdIAAReg(behandling.getId(), behandling.getAktørId(), skjæringstidspunkt);
        //Assert
        assertThat(resultat2).isFalse();
    }

    private void opprettAktørArbeidMedYrkesaktivitet(Behandling behandling, EksternArbeidsforholdRef ref, Arbeidsgiver arbeidsgiver) {
        InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());

        InternArbeidsforholdRef internRef = builder.medNyInternArbeidsforholdRef(arbeidsgiver, ref);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        AktivitetsAvtaleBuilder aktivitetsAvtale = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMed(skjæringstidspunkt.minusYears(2)))
            .medProsentsats(BigDecimal.TEN);
        AktivitetsAvtaleBuilder arbeidsperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMed(skjæringstidspunkt.minusYears(2)));
        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(arbeidsperiode)
            .medArbeidsforholdId(internRef);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeid);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private void sendNyInntektsmelding(Behandling behandling, Arbeidsgiver arbeidsgiver,  EksternArbeidsforholdRef ref) {

        JournalpostId journalpostId = new JournalpostId(1L);
        InntektsmeldingBuilder inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
        .medArbeidsgiver(arbeidsgiver)
        .medArbeidsforholdId(ref)
        .medBeløp(BigDecimal.TEN)
        .medStartDatoPermisjon(skjæringstidspunkt)
        .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.NY)
        .medInnsendingstidspunkt(nyTid()).medJournalpostId(journalpostId);

        inntektsmeldingTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of(inntektsmeldingBuilder));
    }

    private LocalDateTime nyTid() {
        return nåTid.plusSeconds((nåTidTeller++));
    }

    private void sendInnInntektsmelding(Behandling behandling, Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef ref) {
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
        .medArbeidsgiver(arbeidsgiver)
        .medArbeidsforholdId(ref)
        .medBeløp(BigDecimal.TEN)
        .medStartDatoPermisjon(skjæringstidspunkt)
        .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.ENDRING)
        .medInnsendingstidspunkt(nyTid()).medJournalpostId(new JournalpostId("123"));

        inntektsmeldingTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of(inntektsmeldingBuilder));
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        BehandlingLås lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
        FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private Behandling opprettRevurderingsbehandling(Behandling opprinneligBehandling) {
        BehandlingType behandlingType = BehandlingType.REVURDERING;
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medOriginalBehandling(opprinneligBehandling);
        Behandling revurdering = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
            .medBehandlingÅrsak(revurderingÅrsak).build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(opprinneligBehandling.getId(), revurdering.getId());
        return revurdering;
    }

    private String opprettVirksomhet(String orgnummer) {
        return orgnummer;
    }
}
