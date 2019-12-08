package no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.beregningsgrunnlag;

import java.time.LocalDate;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;

public class BeregningAktivitetTestUtil {

    public static BeregningAktivitetAggregatEntitet opprettBeregningAktiviteter(LocalDate skjæringstidspunkt, OpptjeningAktivitetType... opptjeningAktivitetTypes) {
        ÅpenDatoIntervallEntitet periode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(2), skjæringstidspunkt);
        return opprettBeregningAktiviteter(skjæringstidspunkt, periode, opptjeningAktivitetTypes);
    }

    public static BeregningAktivitetAggregatEntitet opprettBeregningAktiviteter(LocalDate skjæringstidspunkt, ÅpenDatoIntervallEntitet periode, OpptjeningAktivitetType... opptjeningAktivitetTypes) {
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt);
        for (OpptjeningAktivitetType aktivitet : opptjeningAktivitetTypes) {
            BeregningAktivitetEntitet beregningAktivitet = BeregningAktivitetEntitet.builder()
                .medPeriode(periode)
                .medOpptjeningAktivitetType(aktivitet)
                .build();
            builder.leggTilAktivitet(beregningAktivitet);
        }
        BeregningAktivitetAggregatEntitet aggregat = builder.build();
        return aggregat;
    }

    public static BeregningAktivitetAggregatEntitet opprettBeregningAktiviteter(LocalDate skjæringstidspunkt, ÅpenDatoIntervallEntitet periode, boolean medDagpenger, boolean ekstraAktivitet) {
        if (medDagpenger) {
            return opprettBeregningAktiviteter(skjæringstidspunkt, periode, OpptjeningAktivitetType.ARBEID, OpptjeningAktivitetType.DAGPENGER);
        } else {
            if (ekstraAktivitet) {
                return opprettBeregningAktiviteter(skjæringstidspunkt, periode, OpptjeningAktivitetType.ARBEID, OpptjeningAktivitetType.SYKEPENGER);
            }
            return opprettBeregningAktiviteter(skjæringstidspunkt, periode, OpptjeningAktivitetType.ARBEID);
        }
    }
}
