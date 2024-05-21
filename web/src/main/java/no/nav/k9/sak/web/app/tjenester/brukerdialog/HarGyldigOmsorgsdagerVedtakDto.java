package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import no.nav.k9.sak.typer.Saksnummer;

import java.time.LocalDate;

record HarGyldigOmsorgsdagerVedtakDto(
    Boolean harInnvilgedeBehandlinger,
    Saksnummer saksnummer,
    LocalDate vedtaksdato
) {
}
