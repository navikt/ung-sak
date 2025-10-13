package no.nav.ung.sak.kontrakt.klage;

public final class TekstValideringRegex {

    public static final String FRITEKST = "^[\\p{Pd}\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]*$";
    public static final String KODEVERK = "^[A-Za-zÆØÅæøå0-9_\\.\\-]+$";
}
