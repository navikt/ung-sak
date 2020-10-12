package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Objects;
import java.util.Set;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class OpptjeningsaktiviteterPerYtelse {

    private final Set<OpptjeningAktivitetType> ekskluderteAktiviteter;

    public OpptjeningsaktiviteterPerYtelse(Set<OpptjeningAktivitetType> ekskluderteAktiviteter) {
        this.ekskluderteAktiviteter = Objects.requireNonNull(ekskluderteAktiviteter, "ekskluderteAktiviteter");
    }

    public boolean erRelevantAktivitet(OpptjeningAktivitetType opptjeningAktivitetType) {
        return erInkludert(opptjeningAktivitetType);
    }

    public boolean erInkludert(OpptjeningAktivitetType opptjeningAktivitetType) {
        return !ekskluderteAktiviteter.contains(opptjeningAktivitetType);
    }
}
