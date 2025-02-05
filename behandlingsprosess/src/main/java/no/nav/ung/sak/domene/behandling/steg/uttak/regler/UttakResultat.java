package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

import java.math.BigDecimal;

public record UttakResultat(BigDecimal utbetalingsgrad, UngdomsytelseUttakAvslagsårsak avslagsårsak) {

    public static UttakResultat forAvslag(UngdomsytelseUttakAvslagsårsak avslagsårsak) {
        return new UttakResultat(BigDecimal.ZERO, avslagsårsak);
    }

    public static UttakResultat forInnvilgelse(BigDecimal grad) {
        return new UttakResultat(grad, null);
    }


}
