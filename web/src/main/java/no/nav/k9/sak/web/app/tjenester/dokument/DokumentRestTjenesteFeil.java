package no.nav.k9.sak.web.app.tjenester.dokument;

import static no.nav.k9.felles.feil.LogLevel.ERROR;
import static no.nav.k9.felles.feil.LogLevel.WARN;

import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.ManglerTilgangFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.felles.jpa.TomtResultatException;

public interface DokumentRestTjenesteFeil extends DeklarerteFeil {
    DokumentRestTjenesteFeil FACTORY = FeilFactory.create(DokumentRestTjenesteFeil.class);

    @ManglerTilgangFeil(feilkode = "FP-909799", feilmelding = "Applikasjon har ikke tilgang til tjeneste.", logLevel = ERROR)
    Feil applikasjonHarIkkeTilgangTilHentJournalpostListeTjeneste(ManglerTilgangException sikkerhetsbegrensning);

    @ManglerTilgangFeil(feilkode = "FP-463438", feilmelding = "Applikasjon har ikke tilgang til tjeneste.", logLevel = ERROR)
    Feil applikasjonHarIkkeTilgangTilHentDokumentTjeneste(ManglerTilgangException sikkerhetsbegrensning);

    @TekniskFeil(feilkode = "FP-595861", feilmelding = "Dokument Ikke Funnet for %s dokumentId= %s", logLevel = WARN, exceptionClass = TomtResultatException.class)
    Feil dokumentIkkeFunnet(JournalpostId journalpostId, String dokumentId, TekniskException dokumentIkkeFunnet);
}
