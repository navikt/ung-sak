package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.k9.kodeverk.iay.AktivitetStatus;

/**
 * Nøkkel som brukes av VurderBehovForÅHindreTilbaketrekkV2 for å unikt identifisere andeler uten å skille på ulike andeler hos samme arbeidsgiver
 */
public class BeregningsresultatAktivitetsnøkkelV2 {
    private final Arbeidsgiver arbeidsgiver;
    private final AktivitetStatus aktivitetStatus;

    BeregningsresultatAktivitetsnøkkelV2(BeregningsresultatAndel andel) {
        this.arbeidsgiver = andel.getArbeidsgiver().orElse(null);
        this.aktivitetStatus = andel.getAktivitetStatus();
    }

    @Override
    public String toString() {
        return "BeregningsresultatAktivitetsnøkkelV2{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", aktivitetStatus=" + aktivitetStatus +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeregningsresultatAktivitetsnøkkelV2)){
            return false;
        }
        BeregningsresultatAktivitetsnøkkelV2 that = (BeregningsresultatAktivitetsnøkkelV2) o;

        return Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(aktivitetStatus, that.aktivitetStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, aktivitetStatus);
    }
}
