package no.nav.ung.sak.web.app.tjenester.behandling.personopplysning;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.felles.typer.PersonIdent;
import no.nav.ung.sak.felles.typer.Saksnummer;

public record AktørerForSak(@NotNull @Valid @Size(max = 10) Set<PersonidenterForAktør> personidenterForSak, @NotNull @Valid Saksnummer saksnummer) {
    protected record PersonidenterForAktør(@NotNull @Valid AktørId aktørId, @NotNull @Valid @Size(max = 20) Set<PersonIdent> personIdent) {
    }
}
