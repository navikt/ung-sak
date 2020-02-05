package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.Beløp;

public class ManuellBehandlingRefusjonGraderingDtoTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.now();
    private final static String ORGNR = "123456780";
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.fra(new VirksomhetEntitet.Builder().medOrgnr(ORGNR).build());
    private final static String ORGNR2 = "123456781";
    private static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.fra(new VirksomhetEntitet.Builder().medOrgnr(ORGNR2).build());
    public static final int GRUNNBELØP = 90_000;
    private static final long ANDELSNR2 = 2L;

    private ManuellBehandlingRefusjonGraderingDtoTjeneste manuellBehandling;

    private BeregningAktivitetAggregatEntitet aktivitetAggregatEntitet;

    @Before
    public void setUp() {
        manuellBehandling = new ManuellBehandlingRefusjonGraderingDtoTjeneste(new FordelBeregningsgrunnlagTjeneste());
        aktivitetAggregatEntitet = BeregningAktivitetAggregatEntitet.builder()
            .leggTilAktivitet(lagAktivitet(ARBEIDSGIVER))
            .leggTilAktivitet(lagAktivitet(ARBEIDSGIVER2))
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING).build();
    }

    private BeregningAktivitetEntitet lagAktivitet(Arbeidsgiver arbeidsgiver) {
        return BeregningAktivitetEntitet.builder()
            .medArbeidsgiver(arbeidsgiver).medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID).medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(12), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1))).build();
    }

    @Test
    public void skalKunneEndreInntektEtterRedusertRefusjonTilUnder6G() {
        // Arrange
        AndelGradering graderinger = lagGradering();
        BeregningsgrunnlagEntitet bgFørFordeling = lagBeregningsgrunnlagFørFordeling();

        List<Inntektsmelding> inntektsmeldinger = lagInntektsmeldingOver6GRefusjon();

        // Act
        boolean kreverManuellBehandling = manuellBehandling.skalSaksbehandlerRedigereInntekt(aktivitetAggregatEntitet,
            new AktivitetGradering(graderinger), bgFørFordeling.getBeregningsgrunnlagPerioder().get(0), inntektsmeldinger);

        // Assert
        assertThat(kreverManuellBehandling).isTrue();
    }

    @Test
    public void skalKunneEndreRefusjonEtterRedusertRefusjonTilUnder6G() {
        // Arrange
        AndelGradering graderinger = lagGradering();
        BeregningsgrunnlagEntitet bgFørFordeling = lagBeregningsgrunnlagFørFordeling();
        List<Inntektsmelding> inntektsmeldinger = lagInntektsmeldingOver6GRefusjon();

        // Act
        boolean kreverManuellBehandlingAvRefusjon = manuellBehandling.skalSaksbehandlerRedigereRefusjon(
            aktivitetAggregatEntitet,
            new AktivitetGradering(graderinger),
            bgFørFordeling.getBeregningsgrunnlagPerioder().get(0), inntektsmeldinger);

        // Assert
        assertThat(kreverManuellBehandlingAvRefusjon).isTrue();
    }

    private no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering lagGradering() {
        return no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering.builder()
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .leggTilGradering(new AndelGradering.Gradering(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING, TIDENES_ENDE), BigDecimal.valueOf(50)))
            .medArbeidsgiver(ARBEIDSGIVER2)
            .medAndelsnr(ANDELSNR2).build();
    }

    private no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet lagBeregningsgrunnlagFørFordeling() {
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(new Beløp(GRUNNBELØP))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .build();
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode periode = no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER).medRefusjonskravPrÅr(BigDecimal.valueOf(GRUNNBELØP * 7)))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(ANDELSNR2)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER2))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);
        return bg;
    }

    private List<Inntektsmelding> lagInntektsmeldingOver6GRefusjon() {
        return List.of(InntektsmeldingBuilder.builder().medArbeidsgiver(ARBEIDSGIVER).medRefusjon(BigDecimal.valueOf(GRUNNBELØP * 7)).build());
    }


}
