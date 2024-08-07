package no.nav.k9.sak.web.app.tjenester.behandling.personopplysning;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

public record AktørerForSak(@NotNull @Valid @Size(max = 10) Set<PersonidenterForAktør> personidenterForSak, @NotNull @Valid Saksnummer saksnummer) {
    protected record PersonidenterForAktør(@NotNull @Valid AktørId aktørId, @NotNull @Valid @Size(max = 20) Set<PersonIdent> personIdent) {
    }
}
