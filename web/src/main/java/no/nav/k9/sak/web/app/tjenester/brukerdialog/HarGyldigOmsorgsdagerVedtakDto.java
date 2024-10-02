package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import jakarta.validation.Valid;
import no.nav.k9.sak.typer.Saksnummer;

import java.time.LocalDate;

record HarGyldigOmsorgsdagerVedtakDto(
    Boolean harInnvilgedeBehandlinger,
    @Valid Saksnummer saksnummer,
    LocalDate vedtaksdato
) {
}
