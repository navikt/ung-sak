package no.nav.ung.sak.formidling.template.dto.innvilgelse;

import java.math.BigDecimal;

import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

public record TilkjentPeriodeDto(
    PeriodeDto periode,
    int dagsats,
    BigDecimal satsFaktor,
    long gBeløp,
    long årsbeløp
) {
}
