package no.nav.k9.sak.domene.behandling.steg.foreslåresultat;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "FORBRES")
@FagsakYtelseTypeRef
@BehandlingTypeRef("BT-002")
@ApplicationScoped
class ForeslåBehandlingsresultatStegFørstegang extends ForeslåBehandlingsresultatStegFelles {

    ForeslåBehandlingsresultatStegFørstegang() {
        // for CDI proxy
    }

    @Inject
    ForeslåBehandlingsresultatStegFørstegang(BehandlingRepositoryProvider repositoryProvider,
                                             @Any Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste,
                                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, foreslåBehandlingsresultatTjeneste, skjæringstidspunktTjeneste);
    }
}
