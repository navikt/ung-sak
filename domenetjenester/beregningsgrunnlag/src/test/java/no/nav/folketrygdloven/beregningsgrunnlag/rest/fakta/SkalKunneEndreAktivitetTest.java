package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.SkalKunneEndreAktivitet;
import no.nav.k9.kodeverk.iay.AktivitetStatus;

public class SkalKunneEndreAktivitetTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MAY, 10);
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPeriode periode;


    @Before
    public void setUp() {
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING).medGrunnbeløp(BigDecimal.valueOf(600000)).build();
        periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(beregningsgrunnlag);
    }

    @Test
    public void skalIkkjeKunneEndreAktivitetOmLagtTilAvSaksbehandlerOgDagpenger() {
        BeregningsgrunnlagPrStatusOgAndel dagpengeAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.DAGPENGER)
            .medLagtTilAvSaksbehandler(true)
            .build(periode);

        Boolean skalKunneEndreAktivitet = SkalKunneEndreAktivitet.skalKunneEndreAktivitet(dagpengeAndel);

        assertThat(skalKunneEndreAktivitet).isFalse();
    }

    @Test
    public void skalIkkjeKunneEndreAktivitetOmIkkjeLagtTilAvSaksbehandler() {
        BeregningsgrunnlagPrStatusOgAndel frilans = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medLagtTilAvSaksbehandler(false)
            .build(periode);

        Boolean skalKunneEndreAktivitet = SkalKunneEndreAktivitet.skalKunneEndreAktivitet(frilans);

        assertThat(skalKunneEndreAktivitet).isFalse();
    }

    @Test
    public void skalKunneEndreAktivitetOmLagtTilAvSaksbehandler() {
        BeregningsgrunnlagPrStatusOgAndel frilans = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medLagtTilAvSaksbehandler(true)
            .build(periode);

        Boolean skalKunneEndreAktivitet = SkalKunneEndreAktivitet.skalKunneEndreAktivitet(frilans);

        assertThat(skalKunneEndreAktivitet).isTrue();
    }
}
