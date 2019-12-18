package no.nav.foreldrepenger.skjæringstidspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private Instance<SkjæringstidspunktTjeneste> stpTjenester;

    SkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository,
                                          @Any Instance<SkjæringstidspunktTjeneste> stpTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.stpTjenester = stpTjenester;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erYtelseBehandling()) {

            var stpTjeneste = FagsakYtelseTypeRef.Lookup.find(stpTjenester, behandling.getFagsakYtelseType());
            // FIXME K9 Definer skjæringstidspunkt
            var stp = stpTjeneste.orElseThrow().getSkjæringstidspunkter(behandlingId);
            return stp;
        } else {
            // returner tom container for andre behandlingtyper
            // (så ser vi om det evt. er noen call paths som kaller på noen form for skjæringstidspunkt)
            return Skjæringstidspunkt.builder().build();
        }
    }

}
