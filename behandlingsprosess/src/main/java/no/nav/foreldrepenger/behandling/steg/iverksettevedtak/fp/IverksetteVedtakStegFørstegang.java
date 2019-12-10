package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegTilgrensendeFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

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
