package no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg;

import java.time.LocalDate;

import no.nav.ung.sak.typer.AktørId;

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
