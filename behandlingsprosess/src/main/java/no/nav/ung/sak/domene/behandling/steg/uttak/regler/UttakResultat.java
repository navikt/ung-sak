package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

public record UttakResultat(boolean erInnvilget, UngdomsytelseUttakAvslagsårsak avslagsårsak) {

    public static UttakResultat forAvslag(UngdomsytelseUttakAvslagsårsak avslagsårsak) {
        return new UttakResultat(false, avslagsårsak);
    }

    public static UttakResultat forInnvilgelse() {
        return new UttakResultat(true, null);
    }

}
