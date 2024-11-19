package no.nav.ung.sak.mottak.kompletthetssjekk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.k9.oppdrag.kontrakt.oppsummering.Mottaker;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

/**
 * Fellesklasse for gjenbrukte metode av subklasser for {@link KompletthetsjekkerImpl}.
 * <p>
 * Favor composition over inheritance
 */
@Dependent
public class KompletthetsjekkerFelles {

    /**
     * Disse konstantene ligger hardkodet (og ikke i KonfigVerdi), da endring i en eller flere av disse vil
     * sannsynnlig kreve kodeendring
     */
    private BehandlingRepository behandlingRepository;

    KompletthetsjekkerFelles() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerFelles(BehandlingRepositoryProvider provider) {
        this.behandlingRepository = provider.getBehandlingRepository();
    }

    public Behandling hentBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }

    public Optional<LocalDateTime> finnVentefrist(LocalDate ønsketFrist) {
        if (ønsketFrist.isAfter(LocalDate.now())) {
            LocalDateTime ventefrist = ønsketFrist.atStartOfDay();
            return Optional.of(ventefrist);
        }
        return Optional.empty();
    }

    public void sendBrev(Long behandlingId, DokumentMalType dokumentMalType, Mottaker mottaker) {
        // TODO: Send brev? opprette oppgave?
    }

}
