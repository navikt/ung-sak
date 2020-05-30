package no.nav.k9.sak.domene.opptjening.ytelse;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;

public class OpptjeningsperiodeForSaksbehandlingFRISINN {

    public static class Builder extends OpptjeningsperiodeForSaksbehandling.Builder {

        public Builder() {
            kladd = new OpptjeningsperiodeForSaksbehandling();
        }

        @Override
        protected void valider() {
            // Opptjeningsperiode av typen arbeid krever alltid arbeidsgiver
            if (kladd.getOpptjeningAktivitetType() == OpptjeningAktivitetType.ARBEID && kladd.getArbeidsgiver() == null
                && (kladd.getPeriode() == null)) {
                throw new IllegalStateException("Informasjon om arbeidsgiver mangler for " + toString());
            }
        }
    }

}
