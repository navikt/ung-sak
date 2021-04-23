package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;

public class OpptjeningsaktiviteterPerYtelse {

    private final Set<OpptjeningAktivitetType> ekskluderteAktiviteter;

    public OpptjeningsaktiviteterPerYtelse(Set<OpptjeningAktivitetType> ekskluderteAktiviteter) {
        this.ekskluderteAktiviteter = Objects.requireNonNull(ekskluderteAktiviteter, "ekskluderteAktiviteter");
    }

    public boolean erRelevantAktivitet(OpptjeningAktivitetType opptjeningAktivitetType, OppgittOpptjening oppgittOpptjening) {
        if (OpptjeningAktivitetType.FRILANS.equals(opptjeningAktivitetType)) {
            return harOppgittFrilansISøknad(oppgittOpptjening);
        }
        return erInkludert(opptjeningAktivitetType);
    }

    public boolean erInkludert(OpptjeningAktivitetType opptjeningAktivitetType) {
        return !ekskluderteAktiviteter.contains(opptjeningAktivitetType);
    }

    private boolean harOppgittFrilansISøknad(OppgittOpptjening oppgittOpptjening) {
        return Optional.ofNullable(oppgittOpptjening).flatMap(OppgittOpptjening::getFrilans).isPresent();
    }
}
