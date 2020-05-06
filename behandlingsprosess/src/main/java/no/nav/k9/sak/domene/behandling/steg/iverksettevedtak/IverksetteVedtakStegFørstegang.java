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

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-002") // Førstegangsbehandling
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteVedtakStegFørstegang extends IverksetteVedtakStegTilgrensendeFelles {

    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    IverksetteVedtakStegFørstegang() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakStegFørstegang(BehandlingRepositoryProvider repositoryProvider,
                                          @FagsakYtelseTypeRef OpprettProsessTaskIverksett opprettProsessTaskIverksett,
                                          VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                          IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse,
                                          IverksetteVedtakStatistikk metrikker) {
        super(repositoryProvider, tidligereBehandlingUnderIverksettelse, identifiserOverlappendeInfotrygdYtelse, metrikker);
        this.opprettProsessTaskIverksett = opprettProsessTaskIverksett;
    }

    @Override
    protected void iverksetter(Behandling behandling) {
        opprettProsessTaskIverksett.opprettIverksettingstasker(behandling);
    }
}
