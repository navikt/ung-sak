package no.nav.k9.sak.ytelse.ung.uttak;

import java.math.BigDecimal;

import no.nav.k9.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

public record UngdomsytelseUttak(BigDecimal utbetalingsgrad, UngdomsytelseUttakAvslagsårsak avslagsårsak) {

    @Override
    public String toString() {
        return "UngdomsytelseUttak{" +
            "utbetalingsgrad=" + utbetalingsgrad +
            ", avslagsårsak=" + avslagsårsak +
            '}';
    }
}
