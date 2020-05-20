package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.k9.sak.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.k9.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-004") // Revurdering
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteVedtakStegRevurdering extends IverksetteVedtakStegTilgrensendeFelles {
    private Instance<OpprettProsessTaskIverksett> opprettProsessTaskIverksett;
    IverksetteVedtakStegRevurdering() {
        // for CDI proxy
    }
    @Inject
    public IverksetteVedtakStegRevurdering(BehandlingRepositoryProvider repositoryProvider,
                                           @Any Instance<OpprettProsessTaskIverksett> opprettProsessTaskIverksett,
                                           VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                           IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse) {
        super(repositoryProvider, tidligereBehandlingUnderIverksettelse, identifiserOverlappendeInfotrygdYtelse);
        this.opprettProsessTaskIverksett = opprettProsessTaskIverksett;
    }
    @Override
    protected void iverksetter(Behandling behandling) {
        FagsakYtelseTypeRef.Lookup.find(opprettProsessTaskIverksett, behandling.getFagsakYtelseType()).orElseThrow().opprettIverksettingstasker(behandling);
    }
}
