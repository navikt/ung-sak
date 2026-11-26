package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UngStønadstatistikkPeriode(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    @NotNull StønadstatistikkUtfall utfall,
    @NotNull @Size(max = 100) List<@Valid StønadstatistikkInngangsvilkår> inngangsvilkår
) {
}

