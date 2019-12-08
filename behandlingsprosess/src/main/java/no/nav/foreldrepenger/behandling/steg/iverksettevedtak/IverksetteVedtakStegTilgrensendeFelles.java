package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

public abstract class IverksetteVedtakStegTilgrensendeFelles extends IverksetteVedtakStegYtelseFelles {

    private IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse;

    protected IverksetteVedtakStegTilgrensendeFelles() {
        // for CDI proxy
    }

    public IverksetteVedtakStegTilgrensendeFelles(BehandlingRepositoryProvider repositoryProvider,
                                                  VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                                  IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse) {
        super(repositoryProvider, tidligereBehandlingUnderIverksettelse);
        this.identifiserOverlappendeInfotrygdYtelse = identifiserOverlappendeInfotrygdYtelse;
    }

    @Override
    protected void førIverksetting(Behandling behandling, BehandlingVedtak behandlingVedtak) {
        identifiserOverlappendeInfotrygdYtelse.vurderOgLagreEventueltOverlapp(behandling, behandlingVedtak);
    }
}
