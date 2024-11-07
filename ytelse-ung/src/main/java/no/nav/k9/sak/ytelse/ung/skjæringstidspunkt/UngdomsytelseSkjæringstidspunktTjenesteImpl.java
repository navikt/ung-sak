package no.nav.k9.sak.ytelse.ung.skjæringstidspunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.Skjæringstidspunkt.Builder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@ApplicationScoped
public class UngdomsytelseSkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;

    UngdomsytelseSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public UngdomsytelseSkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        return finnFørsteDag(behandlingId);
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Builder builder = Skjæringstidspunkt.builder();
        builder.medUtledetSkjæringstidspunkt(finnFørsteDag(behandlingId));
        return builder.build();
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return Optional.empty();
    }


    private LocalDate finnFørsteDag(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getFagsak().getPeriode().getFomDato();
    }


}
