package no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning;

public record BarnetilleggDto(
    String antall,
    boolean harFlereBarn,
    long grunnsats,
    long grunnsatsTotal
) {
}

