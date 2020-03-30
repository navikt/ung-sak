package no.nav.k9.sak.mottak;

import java.time.LocalDate;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

public interface SøknadMottakTjeneste<V extends InnsendingInnhold> {

    void mottaSøknad(Saksnummer saksnummer, V søknad);

    Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, LocalDate startDato);
}
