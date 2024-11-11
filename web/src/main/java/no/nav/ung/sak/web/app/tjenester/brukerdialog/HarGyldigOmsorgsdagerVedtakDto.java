package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import jakarta.validation.Valid;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.LocalDate;

record HarGyldigOmsorgsdagerVedtakDto(
    Boolean harInnvilgedeBehandlinger,
    @Valid Saksnummer saksnummer,
    LocalDate vedtaksdato
) {
}
