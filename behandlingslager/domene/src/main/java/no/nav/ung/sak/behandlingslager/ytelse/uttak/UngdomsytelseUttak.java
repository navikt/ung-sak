package no.nav.ung.sak.behandlingslager.ytelse.uttak;

import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

public record UngdomsytelseUttak(UngdomsytelseUttakAvslagsårsak avslagsårsak) {
    @Override
    public String toString() {
        return "UngdomsytelseUttak{" +
            "avslagsårsak=" + avslagsårsak +
            '}';
    }
}
