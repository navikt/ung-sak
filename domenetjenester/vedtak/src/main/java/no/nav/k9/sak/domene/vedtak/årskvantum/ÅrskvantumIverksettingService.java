package no.nav.k9.sak.domene.vedtak.årskvantum;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;


@ApplicationScoped
public class ÅrskvantumIverksettingService {

    private Instance<ÅrskvantumDeaktiveringTjeneste> deaktiveringTjeneste;

    public ÅrskvantumIverksettingService() {
        // for CDI
    }

    @Inject
    public ÅrskvantumIverksettingService(@Any Instance<ÅrskvantumDeaktiveringTjeneste> deaktiveringTjeneste) {
        this.deaktiveringTjeneste = deaktiveringTjeneste;
    }

    public void meldIfraOmIverksettingTilÅrskvantum(Behandling behandling) {
        if (FagsakYtelseType.OMP.equals(behandling.getFagsakYtelseType())) {
            hentDeaktiveringstjeneste(behandling).meldIfraOmIverksetting(behandling);
        }
    }

    private ÅrskvantumDeaktiveringTjeneste hentDeaktiveringstjeneste(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsak().getYtelseType();

        return FagsakYtelseTypeRef.Lookup.find(deaktiveringTjeneste, ytelseType)
            .orElseThrow(() -> new IllegalArgumentException("kan ikke deaktivere uttak(årskvantum) for ytelse: " + ytelseType));
    }
}
