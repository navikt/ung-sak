package no.nav.ung.sak.domene.behandling.steg.foreslåresultat;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT;
import static no.nav.k9.kodeverk.behandling.BehandlingType.FØRSTEGANGSSØKNAD;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(value = FORESLÅ_BEHANDLINGSRESULTAT)
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
