package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;

public record FødselOgDødInfo(
    AktørId aktørId,
    LocalDate fødselsdato,
    LocalDate dødsdato
) {

    @Override
    public String toString() {
        return "FødselOgDødInfo{" +
            "aktørId=" + aktørId +
            ", fødselsdato=" + fødselsdato +
            ", dødsdato=" + dødsdato +
            '}';
    }
}
