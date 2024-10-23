package no.nav.k9.sak.kontrakt;

public final class Patterns {
    public static final String FRITEKST =
        "^[\\p{Graph}\\p{IsWhite_Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$";
    public static final String FRITEKSTBREV =
        "^[\\p{Graph}\\p{IsWhite_Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§\\p{Pd}]*$";

    public static final String BOKSTAVER_OG_TALL_UTEN_WHITESPACE_OG_SPESIALTEGN = "^[\\p{L}\\p{N}]+$";

    private Patterns() {
    }
}
