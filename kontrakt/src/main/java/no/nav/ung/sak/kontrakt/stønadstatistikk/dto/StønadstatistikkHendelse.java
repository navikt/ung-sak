package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StønadstatistikkHendelse(
    @NotNull @Valid FagsakYtelseType ytelseType,
    @NotNull @Valid AktørId søkerAktørId,
    @NotNull @Valid Saksnummer saksnummer,
    @NotNull UUID behandlingUuid,
    UUID forrigeBehandlingUuid,
    @NotNull LocalDate førsteSøknadsdato,
    @NotNull LocalDateTime vedtakstidspunkt,
    @NotNull @Size(min = 1, max = 30) @Pattern(regexp = "^[A-Za-z0-9+/=]+$") String utbetalingsreferanse,
    @Valid UngdomsprogramDeltakelsePeriode ungdomsprogramDeltakelsePeriode,
    @NotNull @Size(max = 100) List<@Valid StønadstatistikkPeriode> behandlingsperioder,
    @NotNull @Size(max = 100) List<@Valid StønadsstatistikkSatsPeriode> satsPerioder,
    @NotNull @Size(max = 100) List<@Valid StønadsstatistikkTilkjentYtelsePeriode> tilkjentYtelsePerioder,
    @NotNull @Size(max = 100) List<@Valid StønadstatistikkInntektPeriode> inntektPerioder
) {
}
