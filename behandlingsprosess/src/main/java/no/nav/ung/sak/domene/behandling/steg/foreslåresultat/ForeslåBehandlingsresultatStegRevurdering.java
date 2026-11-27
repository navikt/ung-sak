package no.nav.ung.sak.domene.behandling.steg.foreslåresultat;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT;
import static no.nav.ung.kodeverk.behandling.BehandlingType.KONTROLL;
import static no.nav.ung.kodeverk.behandling.BehandlingType.REVURDERING;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

/**
 * Brukes også av kontrollbehandlingen. Der utledes resultatet fra vilkårene som er kopiert fra siste vedtak, som for
 * en revurdering med bare inntektskontroll.
 */
@BehandlingStegRef(value = FORESLÅ_BEHANDLINGSRESULTAT)
@FagsakYtelseTypeRef
@BehandlingTypeRef(REVURDERING)
@BehandlingTypeRef(KONTROLL)
@ApplicationScoped
public class ForeslåBehandlingsresultatStegRevurdering extends ForeslåBehandlingsresultatStegFelles {

    ForeslåBehandlingsresultatStegRevurdering() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBehandlingsresultatStegRevurdering(BehandlingRepositoryProvider repositoryProvider,
                                                     @Any Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste) {
        super(repositoryProvider, foreslåBehandlingsresultatTjeneste);
    }

}
