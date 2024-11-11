package no.nav.ung.sak.mottak;

import java.time.LocalDate;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.ung.sak.typer.AktørId;

public interface SøknadMottakTjeneste<V extends InnsendingInnhold> {

    Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato);
}
