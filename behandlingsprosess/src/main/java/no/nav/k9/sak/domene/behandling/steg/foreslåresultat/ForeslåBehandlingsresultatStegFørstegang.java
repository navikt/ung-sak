package no.nav.k9.sak.domene.behandling.steg.foreslåresultat;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT;
import static no.nav.k9.kodeverk.behandling.BehandlingType.FØRSTEGANGSSØKNAD;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(stegtype = FORESLÅ_BEHANDLINGSRESULTAT)
@FagsakYtelseTypeRef
@BehandlingTypeRef(FØRSTEGANGSSØKNAD)
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
