package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

public record AvslåttAndreLivsoppholdsytelser(
    // Bøyd ytelsesnavn, f.eks. "dagpenger". Null når ytelsen ikke navngis i brevet.
    String ytelse,
    boolean annenYtelse,
    String fritekstBrev
) {
    public static AvslåttAndreLivsoppholdsytelser medKunFritekst(String fritekstBrev) {
        return new AvslåttAndreLivsoppholdsytelser(null, false, fritekstBrev);
    }
}
