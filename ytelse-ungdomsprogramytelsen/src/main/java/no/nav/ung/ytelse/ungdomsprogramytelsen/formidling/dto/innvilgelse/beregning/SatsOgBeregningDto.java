package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.innvilgelse.beregning;

public record SatsOgBeregningDto(
    int aldersgrenseSatsendring,
    boolean kunHøySats,
    long grunnbeløp,
    BeregningDto beregning,
    BeregningDto overgangTilHøySats,
    BarnetilleggDto barnetillegg
) {
}
