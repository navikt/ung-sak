package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

public record AvslåttBistand(
    boolean harIkke14aVedtak,
    String fritekstBrev
) {
    public static AvslåttBistand medKunFritekst(String fritekstBrev) {
        return new AvslåttBistand(false, fritekstBrev);
    }
}
