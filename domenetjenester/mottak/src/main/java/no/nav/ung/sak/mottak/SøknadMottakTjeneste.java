package no.nav.ung.sak.mottak;

import java.time.LocalDate;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.ung.sak.typer.AktørId;

public interface SøknadMottakTjeneste<V extends InnsendingInnhold> {

    Fagsak finnEksisterendeFagsak(FagsakYtelseType ytelseType,
                                  AktørId søkerAktørId);

    Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, LocalDate startDato, LocalDate sluttDato);
}
