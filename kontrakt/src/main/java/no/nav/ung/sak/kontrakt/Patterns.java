package no.nav.ung.sak.kontrakt;

import no.nav.k9.felles.validering.InputValideringRegex;

public final class Patterns {
    public static final String FRITEKST = InputValideringRegex.FRITEKST;
    public static final String FRITEKSTBREV =
        "^[\\p{Graph}\\p{IsWhite_Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§\\p{P}]*$";

    public static final String FRITEKST_MISMATCH_MELDING = "matcher ikke tillatt pattern [{regexp}]";

    private Patterns() {}
}
