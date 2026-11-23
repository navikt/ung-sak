package no.nav.ung.sak.kontrakt;

public final class Patterns {
    // \p{Graph} matcher kun ASCII. \p{P} slipper gjennom typografisk tegnsetting (« » “ ” ’ … • –) som tekstbehandlere setter inn.
    public static final String FRITEKST =
        "^[\\p{Graph}\\p{IsWhite_Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§\\p{P}]+$";
    public static final String FRITEKSTBREV =
        "^[\\p{Graph}\\p{IsWhite_Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§\\p{P}]*$";

    public static final String FRITEKST_MISMATCH_MELDING = "matcher ikke tillatt pattern [{regexp}]";

    private Patterns() {}
}
