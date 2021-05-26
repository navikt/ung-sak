package no.nav.k9.sak.mottak;

import java.time.LocalDate;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

public interface SøknadMottakTjeneste<V extends InnsendingInnhold> {

    default Behandling mottaSøknad(Saksnummer saksnummer, JournalpostId journalpostId, V søknad) {
        throw new IllegalArgumentException("Dette endepunktet er deprecated for alle andre ytelser enn Frisinn: Bruk /fordel/journalposter istedenfor.");
    }

    Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato);
}
