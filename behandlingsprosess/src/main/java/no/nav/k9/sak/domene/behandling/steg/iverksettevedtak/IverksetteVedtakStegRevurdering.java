package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.k9.sak.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.k9.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-004") // Revurdering
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteVedtakStegRevurdering extends IverksetteVedtakStegTilgrensendeFelles {

    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    IverksetteVedtakStegRevurdering() {
        // for CDI proxy
    }


    @Inject
    public IverksetteVedtakStegRevurdering(BehandlingRepositoryProvider repositoryProvider,
                                           @FagsakYtelseTypeRef OpprettProsessTaskIverksett opprettProsessTaskIverksett,
                                           VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                           IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse, 
                                           SensuKlient sensuKlient) {
        super(repositoryProvider, tidligereBehandlingUnderIverksettelse, identifiserOverlappendeInfotrygdYtelse, sensuKlient);
        this.opprettProsessTaskIverksett = opprettProsessTaskIverksett;
    }

    @Override
    protected void iverksetter(Behandling behandling) {
        opprettProsessTaskIverksett.opprettIverksettingstasker(behandling);
    }
}
