package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Aktivitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivPeriode;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivitetStatusModell;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class MapBeregningAktiviteterFraVLTilRegelTest {

    private static final String ORGNR = "915933149";
    private static final AktørId aktørId = AktørId.dummy();
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, 1, 1);

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

    private BeregningInntektsmeldingTestUtil beregningInntektsmeldingTestUtil = new BeregningInntektsmeldingTestUtil(inntektsmeldingTjeneste);

    private TestScenarioBuilder scenario;
    private BehandlingReferanse behandlingReferanse;

    private MapBeregningAktiviteterFraVLTilRegel mapper;

    @Before
    public void setup() {
        scenario = TestScenarioBuilder.nyttScenario();
        mapper = new MapBeregningAktiviteterFraVLTilRegel();
        behandlingReferanse = lagReferanse();
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening)
            .medSkjæringstidspunkt(Skjæringstidspunkt.builder().medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT).build());
    }

    @Test
    public void skal_mappe_arbeidsforhold_med_virksomhetarbeidsgiver_fra_opptjening_med_info_i_iay() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusMonths(12);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        Periode periode = Periode.of(fom, tom);

        var opptjeningAktivitet = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, periode, ORGNR, arbId);

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktivitet, List.of());

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        AktivPeriode aktivPeriode = modell.getAktivePerioder().get(0);
        assertThat(aktivPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(aktivPeriode.getPeriode().getTom()).isEqualTo(tom);
        Arbeidsforhold arbeidsforhold = aktivPeriode.getArbeidsforhold();
        assertThat(arbeidsforhold.getAktivitet()).isEqualByComparingTo(Aktivitet.ARBEIDSTAKERINNTEKT);
        assertThat(arbeidsforhold.getReferanseType())
            .isEqualByComparingTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.ReferanseType.ORG_NR);
        assertThat(arbeidsforhold.getOrgnr()).isEqualTo(ORGNR);
        assertThat(arbeidsforhold.getArbeidsforholdId()).isEqualTo(null);
    }

    private AktivitetStatusModell mapForSkjæringstidspunkt(BehandlingReferanse ref, OpptjeningAktiviteter opptjeningAktiviteter,
                                                           Inntektsmelding inntektsmelding) {
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medInntektsmeldinger(inntektsmelding).build();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, null);
        return mapper.mapForSkjæringstidspunkt(input);
    }

    private AktivitetStatusModell mapForSkjæringstidspunkt(BehandlingReferanse ref, OpptjeningAktiviteter opptjeningAktiviteter,
                                                           List<Inntektsmelding> inntektsmeldinger) {
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, null);
        return mapper.mapForSkjæringstidspunkt(input);
    }

    @Test
    public void skal_mappe_arbeidsforhold_med_virksomhetarbeidsgiver_fra_opptjening_med_info_i_iay_med_inntektsmelding() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();

        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusMonths(12);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var periode = Periode.of(fom, tom);

        behandlingReferanse = lagre(scenario);
        var opptjeningAktivitet = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, periode, ORGNR, arbId);

        var inntektsmelding = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR, arbId, SKJÆRINGSTIDSPUNKT, LocalDateTime.now());

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktivitet, inntektsmelding);

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        AktivPeriode aktivPeriode = modell.getAktivePerioder().get(0);
        assertThat(aktivPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(aktivPeriode.getPeriode().getTom()).isEqualTo(tom);
        Arbeidsforhold arbeidsforhold = aktivPeriode.getArbeidsforhold();
        assertThat(arbeidsforhold.getAktivitet()).isEqualByComparingTo(Aktivitet.ARBEIDSTAKERINNTEKT);
        assertThat(arbeidsforhold.getReferanseType())
            .isEqualByComparingTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.ReferanseType.ORG_NR);
        assertThat(arbeidsforhold.getOrgnr()).isEqualTo(ORGNR);
        assertThat(arbeidsforhold.getArbeidsforholdId()).isEqualTo(arbId.getReferanse());
    }

    @Test
    public void skal_mappe_arbeidsforhold_med_virksomhetarbeidsgiver_fra_opptjening_uten_info_i_iay() {
        // Arrange
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusMonths(12);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        Periode periode = Periode.of(fom, tom);

        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, periode, ORGNR, null);

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktiviteter, Collections.emptyList());

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        AktivPeriode aktivPeriode = modell.getAktivePerioder().get(0);
        assertThat(aktivPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(aktivPeriode.getPeriode().getTom()).isEqualTo(tom);
        Arbeidsforhold arbeidsforhold = aktivPeriode.getArbeidsforhold();
        assertThat(arbeidsforhold.getAktivitet()).isEqualByComparingTo(Aktivitet.ARBEIDSTAKERINNTEKT);
        assertThat(arbeidsforhold.getReferanseType())
            .isEqualByComparingTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.ReferanseType.ORG_NR);
        assertThat(arbeidsforhold.getOrgnr()).isEqualTo(ORGNR);
        assertThat(arbeidsforhold.getArbeidsforholdId()).isNull();
    }

    @Test
    public void skal_mappe_arbeidsforhold_fra_samme_arbeidsgiver_med_inntektsmelding_i_iay() {
        // Arrange
        String orgnr = ORGNR;
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        var arbId3 = InternArbeidsforholdRef.nyRef();

        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusMonths(12);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.minusDays(1);

        Periode periode = Periode.of(fom, tom);

        var opptj1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, periode, orgnr, null, arbId1);
        var opptj2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, periode, orgnr, null, arbId2);
        var opptj3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, periode, orgnr, null, arbId3);

        behandlingReferanse = lagre(scenario);

        var im2 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr, arbId2, SKJÆRINGSTIDSPUNKT, LocalDateTime.now().plusSeconds(1));
        var im1 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr, arbId1, SKJÆRINGSTIDSPUNKT, LocalDateTime.now());
        var im3 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr, arbId3, SKJÆRINGSTIDSPUNKT, LocalDateTime.now().plusSeconds(3));

        List<InternArbeidsforholdRef> arbeidsforholdRef = List.of(arbId1, arbId2, arbId3);

        // Sjekke at vi har riktig antall arbeidsforholdref
        assertThat(arbeidsforholdRef).hasSize(3);

        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2, opptj3));
        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktiviteter, List.of(im1, im2, im3));

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(3);
        for (InternArbeidsforholdRef ref : arbeidsforholdRef) {
            Optional<AktivPeriode> aktivPeriodeOpt = modell.getAktivePerioder().stream()
                .filter(ap -> Objects.equals(ap.getArbeidsforhold().getArbeidsforholdId(), ref.getReferanse()))
                .findFirst();
            assertThat(aktivPeriodeOpt.isPresent()).isTrue();
            AktivPeriode aktivPeriode = aktivPeriodeOpt.get();
            assertThat(aktivPeriode.getPeriode().getFom()).isEqualTo(fom);
            assertThat(aktivPeriode.getPeriode().getTom()).isEqualTo(tom);
            Arbeidsforhold arbeidsforhold = aktivPeriode.getArbeidsforhold();
            assertThat(arbeidsforhold.getAktivitet()).isEqualByComparingTo(Aktivitet.ARBEIDSTAKERINNTEKT);
            assertThat(arbeidsforhold.getReferanseType())
                .isEqualByComparingTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.ReferanseType.ORG_NR);
            assertThat(arbeidsforhold.getOrgnr()).isEqualTo(orgnr);
        }
    }

    @Test
    public void skal_mappe_til_kun_en_aktivitet_med_fleire_arbeidsforhold_for_samme_arbeidsgiver_i_iay_uten_inntektsmelding() {
        // Arrange
        String orgnr = ORGNR;
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        var arbId3 = InternArbeidsforholdRef.nyRef();

        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusMonths(12);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        Periode periode = Periode.of(fom, tom);

        var opptj1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, periode, orgnr, null, arbId1);
        var opptj2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, periode, orgnr, null, arbId2);
        var opptj3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, periode, orgnr, null, arbId3);

        behandlingReferanse = lagre(scenario);

        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2, opptj3));

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktiviteter, List.of());

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        AktivPeriode aktivPeriode = modell.getAktivePerioder().get(0);
        assertThat(aktivPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(aktivPeriode.getPeriode().getTom()).isEqualTo(tom);
        Arbeidsforhold arbeidsforhold = aktivPeriode.getArbeidsforhold();
        assertThat(arbeidsforhold.getArbeidsforholdId()).isNull();
        assertThat(arbeidsforhold.getAktivitet()).isEqualByComparingTo(Aktivitet.ARBEIDSTAKERINNTEKT);
        assertThat(arbeidsforhold.getReferanseType())
            .isEqualByComparingTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.ReferanseType.ORG_NR);
        assertThat(arbeidsforhold.getOrgnr()).isEqualTo(ORGNR);
    }

    @Test
    public void skal_mappe_til_kun_en_aktivitet_med_fleire_arbeidsforhold_i_iay_for_samme_arbeidsgiver_med_felles_inntektsmelding() {
        // Arrange
        String orgnr = ORGNR;
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        var arbId3 = InternArbeidsforholdRef.nyRef();

        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusMonths(12);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        Periode periode = Periode.of(fom, tom);

        var opptj1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, periode, orgnr, null, arbId1);
        var opptj2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, periode, orgnr, null, arbId2);
        var opptj3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, periode, orgnr, null, arbId3);

        behandlingReferanse = lagre(scenario);

        var im1 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR, null, SKJÆRINGSTIDSPUNKT, LocalDateTime.now());
        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2, opptj3));

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktiviteter, List.of(im1));

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        AktivPeriode aktivPeriode = modell.getAktivePerioder().get(0);
        assertThat(aktivPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(aktivPeriode.getPeriode().getTom()).isEqualTo(tom);
        Arbeidsforhold arbeidsforhold = aktivPeriode.getArbeidsforhold();
        assertThat(arbeidsforhold.getArbeidsforholdId()).isNull();
        assertThat(arbeidsforhold.getAktivitet()).isEqualByComparingTo(Aktivitet.ARBEIDSTAKERINNTEKT);
        assertThat(arbeidsforhold.getReferanseType())
            .isEqualByComparingTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.ReferanseType.ORG_NR);
        assertThat(arbeidsforhold.getOrgnr()).isEqualTo(ORGNR);
    }

    @Test
    public void skal_mappe_arbeidsforhold_med_privatpersonarbeidsgiver_fra_opptjening() {
        // Arrange
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusMonths(12);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var periode = Periode.of(fom, tom);

        var opptjeningAktivitet = OpptjeningAktiviteter.fraAktørId(OpptjeningAktivitetType.ARBEID, periode, aktørId.getId());

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktivitet, Collections.emptyList());

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        AktivPeriode aktivPeriode = modell.getAktivePerioder().get(0);
        assertThat(aktivPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(aktivPeriode.getPeriode().getTom()).isEqualTo(tom);
        Arbeidsforhold arbeidsforhold = aktivPeriode.getArbeidsforhold();
        assertThat(arbeidsforhold.getAktivitet()).isEqualByComparingTo(Aktivitet.ARBEIDSTAKERINNTEKT);
        assertThat(arbeidsforhold.getReferanseType())
            .isEqualByComparingTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.ReferanseType.AKTØR_ID);
        assertThat(arbeidsforhold.getAktørId()).isEqualTo(aktørId.getId());
        assertThat(arbeidsforhold.getArbeidsforholdId()).isNull();
    }

    private BehandlingReferanse lagReferanse() {
        var skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT)
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT).build();

        return BehandlingReferanse.fra(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, aktørId, new Saksnummer("1"),
            1L, 1L, UUID.randomUUID(), Optional.empty(), BehandlingStatus.UTREDES, skjæringstidspunkt);
    }

    @Test
    public void skal_mappe_frilansaktivitet_for_opptjening() {
        // Arrange

        Periode periode = Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(12), SKJÆRINGSTIDSPUNKT.minusMonths(6));

        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.FRILANS, periode);

        behandlingReferanse = lagre(scenario);

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktiviteter, Collections.emptyList());

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        assertFrilansPeriode(modell, periode);
    }

    @Test
    public void skal_mappe_alle_SN_fra_opptjening_til_ein_aktivitet() {
        // Arrange
        var opptj1 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.NÆRING,
            Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(12), SKJÆRINGSTIDSPUNKT.minusMonths(6)), "674367833");
        var opptj2 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.NÆRING,
            Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(4), SKJÆRINGSTIDSPUNKT.minusMonths(2)), "5465464545");
        var opptj3 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.NÆRING,
            Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.plusMonths(4)), "543678342");

        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2, opptj3));

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktiviteter, List.of());

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        Periode forventetPeriode = Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(12), SKJÆRINGSTIDSPUNKT.plusMonths(4));
        assertNæringPeriode(modell, forventetPeriode);
    }

    @Test
    public void skal_mappe_sykepenger_fra_opptjening_til_ein_aktivitet() {
        // Arrange
        var opptj1 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.SYKEPENGER,
            Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(12), SKJÆRINGSTIDSPUNKT.minusMonths(6)), "674367833");
        var opptj2 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.SYKEPENGER,
            Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(4), SKJÆRINGSTIDSPUNKT.minusMonths(2)), "5465464545");
        var opptj3 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.SYKEPENGER,
            Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.plusMonths(4)), "543678342");

        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2, opptj3));

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktiviteter, List.of());

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        Periode forventetPeriode = Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(12), SKJÆRINGSTIDSPUNKT.plusMonths(4));
        assertSykepengerPeriode(modell, forventetPeriode);
    }

    @Test
    public void skal_ikkje_mappe_etterutdanning() {
        // Arrange
        Periode periode = Periode.of(SKJÆRINGSTIDSPUNKT.minusMonths(12), SKJÆRINGSTIDSPUNKT);

        var opptj1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.VENTELØNN_VARTPENGER, periode);
        var opptj2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.VIDERE_ETTERUTDANNING, periode);

        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2));

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktiviteter, Collections.emptyList());

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(1);
        assertThat(modell.getAktivePerioder().get(0).getAktivitet()).isEqualTo(Aktivitet.VENTELØNN_VARTPENGER);
    }

    @Test
    public void skal_mappe_arbeidsforhold_med_virksomhetarbeidsgiver_fra_iay_som_ikkje_finnes_i_aareg() {

        behandlingReferanse = scenario.lagre(repositoryProvider).medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT)
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .build());

        // Arrange
        String orgnr1 = ORGNR;
        var arbId = InternArbeidsforholdRef.nyRef();
        LocalDate fom = SKJÆRINGSTIDSPUNKT.minusMonths(12);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var opptj1 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, Periode.of(fom, tom), orgnr1, null, arbId);
        var im1 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr1, arbId, SKJÆRINGSTIDSPUNKT, LocalDateTime.now());

        // Bygg arbeid som ikkje ligger i opptjening
        // Arbeidsforhold starter på skjæringstidspunkt for opptjening. Skal ikkje vere med i mappinga.
        String orgnr2 = "23478497234";
        var arbId2 = InternArbeidsforholdRef.nyRef();
        LocalDate fom2 = SKJÆRINGSTIDSPUNKT;
        LocalDate tom2 = SKJÆRINGSTIDSPUNKT.plusMonths(2);
        var opptj2 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, Periode.of(fom2, tom2), orgnr2, null, arbId2);
        var im2 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr2, arbId2, SKJÆRINGSTIDSPUNKT, LocalDateTime.now().plusSeconds(1));

        // Arbeidsforhold starter før skjæringstidspunktet og slutter etter. Skal vere med i mapping.
        String orgnr3 = "874893579834";
        var arbId3 = InternArbeidsforholdRef.nyRef();
        LocalDate fom3 = SKJÆRINGSTIDSPUNKT.minusMonths(3);
        LocalDate tom3 = SKJÆRINGSTIDSPUNKT.plusMonths(1);
        var opptj3 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, Periode.of(fom3, tom3), orgnr3, null, arbId3);
        var im3 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr3, arbId3, SKJÆRINGSTIDSPUNKT, LocalDateTime.now().plusSeconds(2));

        // Arbeidsforhold starter etter skjæringstidspunktet. Skal ikkje vere med i mappinga.
        String orgnr4 = "789458734893";
        var arbId4 = InternArbeidsforholdRef.nyRef();
        LocalDate fom4 = SKJÆRINGSTIDSPUNKT.plusMonths(1);
        LocalDate tom4 = SKJÆRINGSTIDSPUNKT.plusMonths(2);
        var opptj4 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, Periode.of(fom4, tom4), orgnr4, null, arbId4);
        var im4 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr4, arbId4, SKJÆRINGSTIDSPUNKT, LocalDateTime.now());

        // Arbeidsforhold starter før skjæringstidspunktet og slutter dagen før skjæringstidspunktet. Skal vere med i mappinga.
        String orgnr5 = "435348734893";
        var arbId5 = InternArbeidsforholdRef.nyRef();
        LocalDate fom5 = SKJÆRINGSTIDSPUNKT.minusMonths(6);
        LocalDate tom5 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var opptj5 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, Periode.of(fom5, tom5), orgnr5, null, arbId5);
        var im5 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr5, arbId5, SKJÆRINGSTIDSPUNKT, LocalDateTime.now().plusSeconds(1));

        // Arbeidsforhold starter før skjæringstidspunktet og slutter på skjæringstidspunktet. Skal vere med i mappinga.
        String orgnr6 = "543534348734893";
        var arbId6 = InternArbeidsforholdRef.nyRef();
        LocalDate fom6 = SKJÆRINGSTIDSPUNKT.minusMonths(6);
        LocalDate tom6 = SKJÆRINGSTIDSPUNKT;
        var opptj6 = OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.ARBEID, Periode.of(fom6, tom6), orgnr6, null, arbId6);
        var im6 = beregningInntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr6, arbId6, SKJÆRINGSTIDSPUNKT, LocalDateTime.now().plusSeconds(2));

        // Act
        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj1, opptj2, opptj3, opptj4, opptj5, opptj6));

        // Act
        AktivitetStatusModell modell = mapForSkjæringstidspunkt(behandlingReferanse, opptjeningAktiviteter, List.of(im1, im2, im3, im4, im5, im6));

        // Assert
        assertThat(modell.getSkjæringstidspunktForOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(modell.getAktivePerioder()).hasSize(4);
        assertAktivPeriode(modell, orgnr1, fom, tom, arbId.getReferanse());
        assertAktivPeriode(modell, orgnr3, fom3, tom3, arbId3.getReferanse());
        assertAktivPeriode(modell, orgnr5, fom5, tom5, arbId5.getReferanse());
        assertAktivPeriode(modell, orgnr6, fom6, tom6, arbId6.getReferanse());
    }

    private void assertSykepengerPeriode(AktivitetStatusModell modell, Periode periode) {
        Optional<AktivPeriode> aktivPeriodeOpt = modell.getAktivePerioder().stream()
            .filter(ap -> ap.getPeriode().equals(periode)).findFirst();
        assertThat(aktivPeriodeOpt.isPresent()).isTrue();
        AktivPeriode aktivPeriode = aktivPeriodeOpt.get();
        assertThat(aktivPeriode.getAktivitet()).isEqualByComparingTo(Aktivitet.SYKEPENGER_MOTTAKER);
        assertThat(aktivPeriode.getArbeidsforhold()).isNull();
    }

    private void assertNæringPeriode(AktivitetStatusModell modell, Periode periode) {
        Optional<AktivPeriode> aktivPeriodeOpt = modell.getAktivePerioder().stream()
            .filter(ap -> ap.getPeriode().equals(periode)).findFirst();
        assertThat(aktivPeriodeOpt.isPresent()).isTrue();
        AktivPeriode aktivPeriode = aktivPeriodeOpt.get();
        assertThat(aktivPeriode.getAktivitet()).isEqualByComparingTo(Aktivitet.NÆRINGSINNTEKT);
        assertThat(aktivPeriode.getArbeidsforhold()).isNull();
    }

    private void assertFrilansPeriode(AktivitetStatusModell modell, Periode periode) {
        Optional<AktivPeriode> aktivPeriodeOpt = modell.getAktivePerioder().stream()
            .filter(ap -> ap.getPeriode().equals(periode)).findFirst();
        assertThat(aktivPeriodeOpt.isPresent()).isTrue();
        AktivPeriode aktivPeriode = aktivPeriodeOpt.get();
        assertThat(aktivPeriode.getAktivitet()).isEqualByComparingTo(Aktivitet.FRILANSINNTEKT);
        assertThat(aktivPeriode.getArbeidsforhold().getAktivitet()).isEqualByComparingTo(Aktivitet.FRILANSINNTEKT);
        assertThat(aktivPeriode.getArbeidsforhold().getOrgnr()).isNull();
        assertThat(aktivPeriode.getArbeidsforhold().getAktørId()).isNull();
        assertThat(aktivPeriode.getArbeidsforhold().getArbeidsforholdId()).isNull();
    }

    private void assertAktivPeriode(AktivitetStatusModell modell, String orgnr, LocalDate fom, LocalDate tom, String arbRef) {
        Optional<AktivPeriode> aktivPeriodeOpt = modell.getAktivePerioder().stream()
            .filter(ap -> Objects.equals(ap.getArbeidsforhold().getReferanseType(),
                no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.ReferanseType.ORG_NR) &&
                Objects.equals(ap.getArbeidsforhold().getOrgnr(), orgnr) &&
                Objects.equals(ap.getArbeidsforhold().getArbeidsforholdId(), arbRef) &&
                Objects.equals(ap.getArbeidsforhold().getAktivitet(), Aktivitet.ARBEIDSTAKERINNTEKT))
            .findFirst();
        assertThat(aktivPeriodeOpt.isPresent()).isTrue();
        AktivPeriode aktivPeriode = aktivPeriodeOpt.get();
        assertThat(aktivPeriode.getPeriode().getFom()).isEqualTo(fom);
        assertThat(aktivPeriode.getPeriode().getTom()).isEqualTo(tom);
    }
}
