package no.nav.ung.sak.formidling.template.dto.innvilgelse;

import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

public record TilkjentPeriodeDto(
    PeriodeDto periode,
    TilkjentSatsDto tilkjentSats
) {
}
