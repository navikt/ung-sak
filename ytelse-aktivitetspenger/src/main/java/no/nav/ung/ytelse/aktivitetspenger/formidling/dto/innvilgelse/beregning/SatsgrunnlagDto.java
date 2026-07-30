package no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning;

public record SatsgrunnlagDto(
    String faktor,
    long minsteÅrligeYtelse,
    long minstesats
) {
}

