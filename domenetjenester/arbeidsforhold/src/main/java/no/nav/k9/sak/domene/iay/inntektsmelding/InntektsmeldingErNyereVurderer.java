package no.nav.k9.sak.domene.iay.inntektsmelding;

import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

public final class InntektsmeldingErNyereVurderer {

    private static final String ALTINN_SYSTEM_NAVN = "AltinnPortal";

    private InntektsmeldingErNyereVurderer() {
    }

    public static boolean erNyere(Inntektsmelding gammel, Inntektsmelding ny) {
        if (gammel.gjelderSammeArbeidsforhold(ny)) {
            if (ALTINN_SYSTEM_NAVN.equals(gammel.getKildesystem()) || ALTINN_SYSTEM_NAVN.equals(ny.getKildesystem())) {
                // WTF?  Hvorfor trengs ALTINN å spesialbehandles?
                if (gammel.getKanalreferanse() != null && ny.getKanalreferanse() != null) {
                    // skummelt å stole på stigende arkivreferanser fra Altinn. :-(
                    return ny.getKanalreferanse().compareTo(gammel.getKanalreferanse()) > 0;
                }
            }
            if (gammel.getInnsendingstidspunkt().isBefore(ny.getInnsendingstidspunkt())) {
                return true;
            }
            if (gammel.getInnsendingstidspunkt().equals(ny.getInnsendingstidspunkt()) && ny.getKanalreferanse() != null) {
                if (gammel.getKanalreferanse() != null) {
                    // skummelt å stole på stigende arkivreferanser fra Altinn. :-(
                    return ny.getKanalreferanse().compareTo(gammel.getKanalreferanse()) > 0;
                }
                return true;
            }
        }
        return false;
    }
}
