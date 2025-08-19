package no.nav.ung.sak.formidling.template.dto.innvilgelse.beregning;

public record BarnetilleggDto(
    String antall,
    boolean harFlereBarn,
    long grunnsats,
    long grunnsatsTotal
) {
}
