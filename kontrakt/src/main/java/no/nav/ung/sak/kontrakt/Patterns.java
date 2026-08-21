package no.nav.ung.sak.kontrakt;

public final class Patterns {
    public static final String FRITEKST =
        "^[\\p{Graph}\\p{IsWhite_Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$";
    public static final String FRITEKSTBREV =
        "^[\\p{Graph}\\p{IsWhite_Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§\\p{Pd}]*$";

    public static final String FRITEKST_MISMATCH_MELDING = "matcher ikke tillatt pattern [{regexp}]";

    private Patterns() {}
}
