package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningAktiviteterFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Aktivitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivPeriode;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivitetStatusModell;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class MapBeregningAktiviteterFraRegelTilVLTest {

    private static final String ORGNR = "900050001";

    private MapBeregningAktiviteterFraRegelTilVL mapper;

    @Before
    public void setup() {
        mapper = new MapBeregningAktiviteterFraRegelTilVL();
    }

    @Test
    public void mapFrilanserOgArbeidstakerAktiviteter() {
        // Arrange
        AktivitetStatusModell regelmodell = new AktivitetStatusModell();
        LocalDate idag = LocalDate.now();
        regelmodell.setSkjæringstidspunktForOpptjening(idag);
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        LocalDate a0fom = idag.minusMonths(5);
        LocalDate a0tom = idag;
        AktivPeriode frilans = new AktivPeriode(Aktivitet.FRILANSINNTEKT, new Periode(a0fom, a0tom), Arbeidsforhold.frilansArbeidsforhold());
        regelmodell.leggTilEllerOppdaterAktivPeriode(frilans);
        LocalDate a1fom = idag.minusMonths(10);
        LocalDate a1tom = idag.minusMonths(4);
        AktivPeriode arbeidstaker = new AktivPeriode(Aktivitet.ARBEIDSTAKERINNTEKT, new Periode(a1fom, a1tom), Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR, arbeidsforholdRef.getReferanse()));
        regelmodell.leggTilEllerOppdaterAktivPeriode(arbeidstaker);

        // Act
        BeregningAktivitetAggregatEntitet aktivitetAggregat = mapper.map(regelmodell);

        // Assert
        List<BeregningAktivitetEntitet> beregningAktiviteter = aktivitetAggregat.getBeregningAktiviteter();
        assertThat(beregningAktiviteter).hasSize(2);
        assertThat(beregningAktiviteter.get(0)).satisfies(aktivitet -> {
            assertThat(aktivitet.getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.nullRef());
            assertThat(aktivitet.getArbeidsgiver()).isNull();
            assertThat(aktivitet.getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.FRILANS);
            assertThat(aktivitet.getPeriode().getFomDato()).isEqualTo(a0fom);
            assertThat(aktivitet.getPeriode().getTomDato()).isEqualTo(a0tom);
        });
        assertThat(beregningAktiviteter.get(1)).satisfies(aktivitet -> {
            assertThat(aktivitet.getArbeidsforholdRef()).isEqualTo(arbeidsforholdRef);
            assertThat(aktivitet.getArbeidsgiver().getErVirksomhet()).isTrue();
            assertThat(aktivitet.getArbeidsgiver().getIdentifikator()).isEqualTo(ORGNR);
            assertThat(aktivitet.getOpptjeningAktivitetType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
            assertThat(aktivitet.getPeriode().getFomDato()).isEqualTo(a1fom);
            assertThat(aktivitet.getPeriode().getTomDato()).isEqualTo(a1tom);
        });
    }
}
