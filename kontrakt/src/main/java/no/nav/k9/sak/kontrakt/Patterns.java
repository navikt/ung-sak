package no.nav.k9.sak.kontrakt;

public final class Patterns {
    public static final String FRITEKST =
        "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$";
    public static final String FRITEKSTBREV =
        "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§\\p{Pd}]*$";

    private Patterns() {}
}
