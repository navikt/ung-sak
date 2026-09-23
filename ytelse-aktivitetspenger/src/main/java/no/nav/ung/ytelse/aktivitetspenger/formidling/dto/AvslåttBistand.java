package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

public record AvslåttBistand(
    Bistandsårsak årsak,
    String fritekstBrev
) {
    public enum Bistandsårsak {
        HAR_IKKE_14A_VEDTAK
    }
}
