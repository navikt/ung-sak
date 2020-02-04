package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.ArbeidType;
import no.nav.k9.kodeverk.iay.InntektsKilde;
import no.nav.k9.kodeverk.iay.InntektspostType;
import no.nav.k9.kodeverk.iay.SkatteOgAvgiftsregelType;

public class InntektForAndelTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, 9, 30);
    private static final BigDecimal INNTEKT1 = BigDecimal.valueOf(25000);
    private static final BigDecimal INNTEKT2 = BigDecimal.valueOf(30000);
    private static final BigDecimal INNTEKT3 = BigDecimal.valueOf(35000);
    private static final BigDecimal SNITT_AV_ULIKE_INNTEKTER = INNTEKT1.add(INNTEKT2).add(INNTEKT3).divide(BigDecimal.valueOf(3), RoundingMode.HALF_UP);
    public static final String ORGNR = "379472397427";
    private static final InternArbeidsforholdRef ARB_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String FRILANS_OPPDRAG_ORGNR = "784385345";
    private static final String FRILANS_OPPDRAG_ORGNR2 = "748935793457";

    private Arbeidsgiver arbeidsgiver;
    private Arbeidsgiver frilansArbeidsgiver;
    private Arbeidsgiver frilansArbeidsgiver2;
    private Yrkesaktivitet arbeidstakerYrkesaktivitet;
    private Yrkesaktivitet frilansOppdrag;
    private Yrkesaktivitet frilansOppdrag2;
    private Yrkesaktivitet frilans;
    private BeregningsgrunnlagPrStatusOgAndel arbeidstakerAndel;
    private BeregningsgrunnlagPrStatusOgAndel frilansAndel;
    private BeregningsgrunnlagPeriode periode;
    private BehandlingReferanse behandlingReferanse;
    private AbstractTestScenario<?> scenario;


    @Before
    public void setUp() {
        byggArbeidsgiver();
        byggArbeidstakerYrkesaktivitet();
        byggFrilansOppdragAktivitet();
        byggFrilansAktivitet();
        lagBGPeriode();
        lagArbeidstakerAndel();
        lagFrilansAndel();

        scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagMocked();
    }

    @Test
    public void skal_finne_snitt_inntekt_for_arbeidstaker_med_lik_inntekt_pr_mnd() {
        var aktørInntekt = lagAktørInntekt(singletonList(lagLikInntektSiste3Mnd(arbeidsgiver)));
        var filter = new InntektFilter(aktørInntekt.build());
        BigDecimal snittIBeregningsperioden = InntektForAndelTjeneste.finnSnittinntektForArbeidstakerIBeregningsperioden(filter, arbeidstakerAndel);
        assertThat(snittIBeregningsperioden).isEqualByComparingTo(INNTEKT1);
    }

    @Test
    public void skal_finne_snitt_inntekt_for_arbeidstaker_med_ulik_inntekt_pr_mnd() {
        var aktørInntekt = lagAktørInntekt(singletonList(lagUlikInntektSiste3Mnd(arbeidsgiver)));
        var filter = new InntektFilter(aktørInntekt.build());
        BigDecimal snittIBeregningsperioden = InntektForAndelTjeneste.finnSnittinntektForArbeidstakerIBeregningsperioden(filter, arbeidstakerAndel);
        assertThat(snittIBeregningsperioden).isEqualByComparingTo(SNITT_AV_ULIKE_INNTEKTER);
    }

    @Test
    public void skal_finne_snitt_inntekt_for_frilans_med_lik_inntekt_pr_mnd() {
        List<InntektBuilder> inntekter = List.of(lagLikInntektSiste3Mnd(arbeidsgiver), lagLikInntektSiste3Mnd(frilansArbeidsgiver));
        List<Yrkesaktivitet> aktiviteter = List.of(arbeidstakerYrkesaktivitet, frilans, frilansOppdrag);
        var grunnlagEntitet = lagIAYGrunnlagEntitet(inntekter, aktiviteter);
        Optional<BigDecimal> snittIBeregningsperioden = InntektForAndelTjeneste.finnSnittAvFrilansinntektIBeregningsperioden(behandlingReferanse.getAktørId(), grunnlagEntitet, frilansAndel, SKJÆRINGSTIDSPUNKT_OPPTJENING);
        assertThat(snittIBeregningsperioden).hasValueSatisfying(a -> assertThat(a).isEqualByComparingTo(INNTEKT1));
    }

    @Test
    public void skal_finne_snitt_inntekt_for_frilans_med_ulik_inntekt_pr_mnd() {
        List<InntektBuilder> inntekter = List.of(lagLikInntektSiste3Mnd(arbeidsgiver), lagUlikInntektSiste3Mnd(frilansArbeidsgiver));
        List<Yrkesaktivitet> aktiviteter = List.of(arbeidstakerYrkesaktivitet, frilans, frilansOppdrag);
        var grunnlagEntitet = lagIAYGrunnlagEntitet(inntekter, aktiviteter);
        Optional<BigDecimal> snittIBeregningsperioden = InntektForAndelTjeneste.finnSnittAvFrilansinntektIBeregningsperioden(behandlingReferanse.getAktørId(), grunnlagEntitet, frilansAndel, SKJÆRINGSTIDSPUNKT_OPPTJENING);
        assertThat(snittIBeregningsperioden).hasValueSatisfying(a -> assertThat(a).isEqualByComparingTo(SNITT_AV_ULIKE_INNTEKTER));
    }

    @Test
    public void skal_finne_snitt_inntekt_for_frilans_med_fleire_oppdragsgivere() {
        List<InntektBuilder> inntekter = List.of(lagLikInntektSiste3Mnd(arbeidsgiver), lagUlikInntektSiste3Mnd(frilansArbeidsgiver), lagUlikInntektSiste3Mnd(frilansArbeidsgiver2));
        List<Yrkesaktivitet> aktiviteter = List.of(arbeidstakerYrkesaktivitet, frilans, frilansOppdrag, frilansOppdrag2);
        var grunnlagEntitet = lagIAYGrunnlagEntitet(inntekter, aktiviteter);
        Optional<BigDecimal> snittIBeregningsperioden = InntektForAndelTjeneste.finnSnittAvFrilansinntektIBeregningsperioden(behandlingReferanse.getAktørId(), grunnlagEntitet, frilansAndel, SKJÆRINGSTIDSPUNKT_OPPTJENING);
        assertThat(snittIBeregningsperioden).hasValueSatisfying(a -> assertThat(a).isEqualByComparingTo(SNITT_AV_ULIKE_INNTEKTER.multiply(BigDecimal.valueOf(2))));
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagEntitet(List<InntektBuilder> inntekter, List<Yrkesaktivitet> aktiviteter) {
        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørInntektBuilder = aggregatBuilder.getAktørInntektBuilder(scenario.getSøkerAktørId());
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medData(InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
                .leggTilAktørArbeid(lagAktørArbeid(aktiviteter))
                .leggTilAktørInntekt(lagAktørInntekt(aktørInntektBuilder, inntekter)));
        return iayGrunnlag.build();
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder lagAktørArbeid(List<Yrkesaktivitet> aktiviteter) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder builder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(scenario.getSøkerAktørId());
        aktiviteter.forEach(builder::leggTilYrkesaktivitet);
        return builder;
    }

    private AktivitetsAvtaleBuilder lagAktivitetsavtale() {
        return AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(2)));
    }

    private InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder lagAktørInntekt(List<InntektBuilder> inntektList) {
        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder builder = InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder.oppdatere(Optional.empty());
        inntektList.forEach(builder::leggTilInntekt);
        return builder;
    }

    private InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder lagAktørInntekt(InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder builder, List<InntektBuilder> inntektList) {
        inntektList.forEach(builder::leggTilInntekt);
        return builder;
    }

    private InntektBuilder lagLikInntektSiste3Mnd(Arbeidsgiver arbeidsgiver) {
        return InntektBuilder.oppdatere(Optional.empty())
        .leggTilInntektspost(lagInntektspost(INNTEKT1, 1))
        .leggTilInntektspost(lagInntektspost(INNTEKT1, 2))
        .leggTilInntektspost(lagInntektspost(INNTEKT1, 3))
        .medArbeidsgiver(arbeidsgiver)
        .medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING);
    }

    private InntektBuilder lagUlikInntektSiste3Mnd(Arbeidsgiver arbeidsgiver) {
        return InntektBuilder.oppdatere(Optional.empty())
            .leggTilInntektspost(lagInntektspost(INNTEKT1, 1))
            .leggTilInntektspost(lagInntektspost(INNTEKT2, 2))
            .leggTilInntektspost(lagInntektspost(INNTEKT3, 3))
            .medArbeidsgiver(arbeidsgiver)
            .medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING);
    }


    private InntektspostBuilder lagInntektspost(BigDecimal inntekt, int mndFørSkjæringstidspunkt) {
        return InntektspostBuilder.ny().medBeløp(inntekt)
            .medInntektspostType(InntektspostType.LØNN)
            .medPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(mndFørSkjæringstidspunkt).withDayOfMonth(1),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(mndFørSkjæringstidspunkt-1).withDayOfMonth(1).minusDays(1))
            .medSkatteOgAvgiftsregelType(SkatteOgAvgiftsregelType.UDEFINERT);
    }

    private void lagArbeidstakerAndel() {
        arbeidstakerAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregningsperiode(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(3).withDayOfMonth(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.withDayOfMonth(1).minusDays(1))
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(arbeidsgiver))
            .medAndelsnr(1L)
            .build(periode);
    }

    private void lagBGPeriode() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(91425L))
            .build();
        periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(beregningsgrunnlag);
    }

    private void byggArbeidstakerYrkesaktivitet() {
        arbeidstakerYrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ARB_ID)
            .leggTilAktivitetsAvtale(lagAktivitetsavtale())
            .build();
    }

    private void byggArbeidsgiver() {
        arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        frilansArbeidsgiver = Arbeidsgiver.virksomhet(FRILANS_OPPDRAG_ORGNR);
        frilansArbeidsgiver2 = Arbeidsgiver.virksomhet(FRILANS_OPPDRAG_ORGNR2);
    }

    private void lagFrilansAndel() {
        frilansAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medBeregningsperiode(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(3).withDayOfMonth(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.withDayOfMonth(1).minusDays(1))
            .medAndelsnr(2L)
            .build(periode);
    }

    private void byggFrilansAktivitet() {
        frilans = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.FRILANSER)
            .build();
    }

    private void byggFrilansOppdragAktivitet() {
        frilansOppdrag = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER)
            .medArbeidsgiver(frilansArbeidsgiver)
            .leggTilAktivitetsAvtale(lagAktivitetsavtale())
            .build();
        frilansOppdrag2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER)
            .medArbeidsgiver(frilansArbeidsgiver2)
            .leggTilAktivitetsAvtale(lagAktivitetsavtale())
            .build();
    }

}
