package no.nav.k9.sak.dokument.bestill;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class DokumentBehandlingTjeneste {
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    public DokumentBehandlingTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBehandlingTjeneste(BehandlingRepository behandlingRepository,
                                      BehandlingskontrollTjeneste behandlingskontrollTjeneste) {

        Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public void settBehandlingPåVent(Long behandlingId, Venteårsak venteårsak) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        behandlingskontrollTjeneste.settBehandlingPåVentUtenSteg(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT,
            LocalDateTime.now().plusDays(14), venteårsak);
    }

    public void utvidBehandlingsfristManuelt(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, finnNyFristManuelt(behandling));
    }

    void oppdaterBehandlingMedNyFrist(Behandling behandling, LocalDate nyFrist) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(nyFrist);
        behandlingRepository.lagre(behandling, lås);
    }

    LocalDate finnNyFristManuelt(Behandling behandling) {
        return LocalDate.now().plusWeeks(behandling.getType().getBehandlingstidFristUker());
    }

    public void utvidBehandlingsfristManueltMedlemskap(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, utledFristMedlemskap(behandling));

    }

    LocalDate utledFristMedlemskap(Behandling behandling) {
        LocalDate vanligFrist = finnNyFristManuelt(behandling);
        return vanligFrist;
    }

}
