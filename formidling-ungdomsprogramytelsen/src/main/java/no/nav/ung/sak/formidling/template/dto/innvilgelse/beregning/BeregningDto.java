package no.nav.ung.sak.formidling.template.dto.innvilgelse.beregning;

public record BeregningDto(
    String faktor,
    long årsbeløp,
    long grunnsats
) {
}
