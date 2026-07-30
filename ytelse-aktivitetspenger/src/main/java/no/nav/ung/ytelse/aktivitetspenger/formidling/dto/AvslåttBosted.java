package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

public record AvslåttBosted(
    boolean ytelseIkkeTilgjengeligPåBosted,
    boolean ytelseIkkeTilgjengeligPåFolkeregistrertEllerBostedsadresse,
    boolean ytelseIkkePåArbeidsstedStudiested,
    String fritekstBrev
) {
    public static AvslåttBosted medKunFritekst(String fritekstBrev) {
        return new AvslåttBosted(
            false,
            false,
            false,
            fritekstBrev
        );
    }
}
