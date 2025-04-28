package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.ung.sak.ytelse.kontroll.Inntektsresultat;

public record Kontrollresultat(KontrollResultatType type, Inntektsresultat inntektsresultat) {

    public static Kontrollresultat utenInntektresultat(KontrollResultatType type) {
        return new Kontrollresultat(type, null);
    }

}
