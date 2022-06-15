package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import java.util.Set;

import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public interface FinnFeriepengepåvirkendeFagsakerTjeneste {
    Set<Fagsak> finnSakerSomPåvirkerFeriepengerFor(Fagsak fagsak);
}
