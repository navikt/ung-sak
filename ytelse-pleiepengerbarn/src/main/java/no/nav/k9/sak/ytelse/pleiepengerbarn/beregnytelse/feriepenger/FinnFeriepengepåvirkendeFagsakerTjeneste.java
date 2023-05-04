package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import java.util.Set;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public interface FinnFeriepengepåvirkendeFagsakerTjeneste {
    static FinnFeriepengepåvirkendeFagsakerTjeneste finnTjeneste(Instance<FinnFeriepengepåvirkendeFagsakerTjeneste> feriepengepåvirkendeFagsakerTjenester, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(feriepengepåvirkendeFagsakerTjenester, fagsakYtelseType).orElseThrow();
    }

    Set<Fagsak> finnSakerSomPåvirkerFeriepengerFor(BehandlingReferanse behandlingReferanse);
}
