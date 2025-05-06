package no.nav.ung.sak.formidling.template.dto.innvilgelse;

import java.math.BigDecimal;

public record TilkjentSatsDto(
    long dagsats,
    BigDecimal satsFaktor,
    long dagsatsGrunnbeløpFaktor,
    long gBeløp,
    long årsbeløp,
    int antallBarn,
    int barnetilleggSats
) {
}
