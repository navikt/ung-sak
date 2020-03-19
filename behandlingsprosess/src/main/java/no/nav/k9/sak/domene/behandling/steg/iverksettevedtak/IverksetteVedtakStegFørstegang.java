package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.k9.sak.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.k9.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-002") // Førstegangsbehandling
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteVedtakStegFørstegang extends IverksetteVedtakStegTilgrensendeFelles {

    IverksetteVedtakStegFørstegang() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakStegFørstegang(BehandlingRepositoryProvider repositoryProvider,
                                            @SuppressWarnings("unused") @FagsakYtelseTypeRef OpprettProsessTaskIverksett opprettProsessTaskIverksett,
                                            VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                            IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse) {
        super(repositoryProvider, tidligereBehandlingUnderIverksettelse, identifiserOverlappendeInfotrygdYtelse);
    }
}
