package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.k9.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;

public abstract class IverksetteVedtakStegTilgrensendeFelles extends IverksetteVedtakStegYtelseFelles {

    private IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse;

    protected IverksetteVedtakStegTilgrensendeFelles() {
        // for CDI proxy
    }

    public IverksetteVedtakStegTilgrensendeFelles(BehandlingRepositoryProvider repositoryProvider,
                                                  VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                                  IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse, 
                                                  SensuKlient sensuKlient) {
        super(repositoryProvider, tidligereBehandlingUnderIverksettelse, sensuKlient);
        this.identifiserOverlappendeInfotrygdYtelse = identifiserOverlappendeInfotrygdYtelse;
    }

    @Override
    protected void førIverksetting(Behandling behandling, BehandlingVedtak behandlingVedtak) {
        identifiserOverlappendeInfotrygdYtelse.vurderOgLagreEventueltOverlapp(behandling, behandlingVedtak);
    }
}
