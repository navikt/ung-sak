package no.nav.k9.sak.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

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
            var stpTjeneste = getTjeneste(behandling.getFagsakYtelseType());
            // FIXME K9 Definer skjæringstidspunkt
            var stp = stpTjeneste.getSkjæringstidspunkter(behandlingId);
            return stp;
        } else {
            // returner tom container for andre behandlingtyper
            // (så ser vi om det evt. er noen call paths som kaller på noen form for skjæringstidspunkt)
            return Skjæringstidspunkt.builder().build();
        }
    }

    private SkjæringstidspunktTjeneste getTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(stpTjenester, ytelseType).orElseThrow(() -> new UnsupportedOperationException("Har ikke " + SkjæringstidspunktTjeneste.class.getSimpleName() + " for " + ytelseType));
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return getTjeneste(ref.getFagsakYtelseType()).getOpphørsdato(ref);
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        return getTjeneste(ytelseType).utledSkjæringstidspunktForRegisterInnhenting(behandlingId, ytelseType);
    }
    
    @Override
    public boolean harAvslåttPeriode(UUID behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);
        return getTjeneste(behandling.getFagsakYtelseType()).harAvslåttPeriode(behandlingUuid);
    }

}
