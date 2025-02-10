package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

public record UttakAvslagResultat(UngdomsytelseUttakAvslagsårsak avslagsårsak) {

    public static UttakAvslagResultat medÅrsak(UngdomsytelseUttakAvslagsårsak avslagsårsak) {
        return new UttakAvslagResultat(avslagsårsak);
    }

}
