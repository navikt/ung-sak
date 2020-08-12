package no.nav.k9.sak.domene.iay.inntektsmelding;

import java.util.Objects;

import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

public final class InntektsmeldingErNyereVurderer {

    private InntektsmeldingErNyereVurderer() {
    }

    public static boolean erNyere(Inntektsmelding gammel, Inntektsmelding ny) {
        if (gammel.gjelderSammeArbeidsforhold(ny)) {
            // skummelt å stole på stigende arkivreferanser fra Altinn. :-(
            String gammelKanalreferanse = Objects.requireNonNull(gammel.getKanalreferanse(), "Mangler gammel kanalreferanse: " + gammel);
            String nyKanalreferanse = Objects.requireNonNull(ny.getKanalreferanse(), "Mangler ny kanalreferanse: " + ny);
            return nyKanalreferanse.compareTo(gammelKanalreferanse) > 0;
        }
        return false;
    }
}
