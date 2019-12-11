package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk.MapOpptjeningAktivitetFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivPeriode;
import no.nav.folketrygdloven.skjæringstidspunkt.regelmodell.AktivitetStatusModell;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;

public class MapBeregningAktiviteterFraRegelTilVL {

    public MapBeregningAktiviteterFraRegelTilVL() {
    }

    public BeregningAktivitetAggregatEntitet map(AktivitetStatusModell regelmodell) {
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder();
        builder.medSkjæringstidspunktOpptjening(regelmodell.getSkjæringstidspunktForOpptjening());
        regelmodell.getAktivePerioder().forEach(aktivPeriode -> builder.leggTilAktivitet(
            mapAktivPeriode(aktivPeriode)
        ));
        return builder.build();
    }

    private BeregningAktivitetEntitet mapAktivPeriode(AktivPeriode aktivPeriode) {
        var builder = BeregningAktivitetEntitet.builder();
        builder.medOpptjeningAktivitetType(MapOpptjeningAktivitetFraRegelTilVL.map(aktivPeriode.getAktivitet()));
        builder.medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(aktivPeriode.getPeriode().getFom(), aktivPeriode.getPeriode().getTom()));
        var arbeidsforhold = aktivPeriode.getArbeidsforhold();
        if (arbeidsforhold != null) {
            var arbeidsgiver = mapArbeidsgiver(arbeidsforhold);
            builder.medArbeidsgiver(arbeidsgiver);
            var arbeidsforholdRef = InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforholdId());
            builder.medArbeidsforholdRef(arbeidsforholdRef);
        }
        return builder.build();
    }

    private Arbeidsgiver mapArbeidsgiver(Arbeidsforhold arbeidsforhold) {
        return MapArbeidsforholdFraRegelTilVL.map(arbeidsforhold);
    }
}
