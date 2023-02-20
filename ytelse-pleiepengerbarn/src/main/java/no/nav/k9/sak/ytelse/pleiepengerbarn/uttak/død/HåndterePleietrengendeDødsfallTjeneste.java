package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import java.util.Optional;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface HåndterePleietrengendeDødsfallTjeneste {

    static HåndterePleietrengendeDødsfallTjeneste velgTjeneste(Instance<HåndterePleietrengendeDødsfallTjeneste> instanser, BehandlingReferanse referanse) {
        return FagsakYtelseTypeRef.Lookup.find(instanser, referanse.getFagsakYtelseType()).orElseThrow(() -> new IllegalStateException("Fant ikke " + HåndterePleietrengendeDødsfallTjeneste.class.getName() + " for " + referanse.getFagsakYtelseType()));
    }

    Optional<DatoIntervallEntitet> utledUtvidetPeriodeForDødsfall(BehandlingReferanse referanse);

    void utvidPerioderVedDødsfall(BehandlingReferanse referanse);

}
