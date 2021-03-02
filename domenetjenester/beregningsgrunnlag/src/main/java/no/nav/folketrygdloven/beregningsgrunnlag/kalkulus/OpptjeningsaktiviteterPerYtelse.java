package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Objects;
import java.util.Set;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;

public class OpptjeningsaktiviteterPerYtelse {

    private final Set<OpptjeningAktivitetType> ekskluderteAktiviteter;

    public OpptjeningsaktiviteterPerYtelse(Set<OpptjeningAktivitetType> ekskluderteAktiviteter) {
        this.ekskluderteAktiviteter = Objects.requireNonNull(ekskluderteAktiviteter, "ekskluderteAktiviteter");
    }

    public boolean erRelevantAktivitet(OpptjeningAktivitetType opptjeningAktivitetType, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (OpptjeningAktivitetType.FRILANS.equals(opptjeningAktivitetType)) {
            return harOppgittFrilansISøknad(iayGrunnlag);
        }
        return erInkludert(opptjeningAktivitetType);
    }

    public boolean erInkludert(OpptjeningAktivitetType opptjeningAktivitetType) {
        return !ekskluderteAktiviteter.contains(opptjeningAktivitetType);
    }

    private boolean harOppgittFrilansISøknad(InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getOppgittOpptjening().flatMap(OppgittOpptjening::getFrilans).isPresent();
    }
}
