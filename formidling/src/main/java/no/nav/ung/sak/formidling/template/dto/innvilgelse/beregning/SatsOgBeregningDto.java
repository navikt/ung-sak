package no.nav.ung.sak.formidling.template.dto.innvilgelse.beregning;

public record SatsOgBeregningDto(
    int aldersgrenseSatsendring,
    boolean kunHøySats,
    long grunnbeløp,
    BeregningDto beregning,
    BeregningDto overgangTilHøySats,
    BarnetilleggDto barnetillegg
) {
}
