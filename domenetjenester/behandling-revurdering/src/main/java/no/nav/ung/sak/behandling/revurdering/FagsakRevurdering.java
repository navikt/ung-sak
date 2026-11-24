package no.nav.ung.sak.behandling.revurdering;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

class FagsakRevurdering {

    private BehandlingRepository behandlingRepository;

    public FagsakRevurdering(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    /**
     * Revurdering kan opprettes når fagsaken har vedtak og ingen åpen ordinær behandling. En åpen kontrollbehandling
     * hindrer ikke opprettelse, fordi de to kan behandles parallelt.
     */
    boolean kanRevurderingOpprettes(Fagsak fagsak) {
        if (harÅpenOrdinærBehandling(fagsak)) {
            return false;
        }
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsak.getSaksnummer());
        return kanRevurderingOpprettes(behandlinger);
    }

    /**
     * Kontrollbehandling kan opprettes når fagsaken har vedtak og ingen kontrollbehandling som ikke er avsluttet. En
     * kontrollbehandling under iverksetting teller som ikke avsluttet, som i den unike indeksen for åpen
     * kontrollbehandling. En åpen ordinær behandling hindrer ikke opprettelse.
     */
    boolean kanKontrollbehandlingOpprettes(Fagsak fagsak) {
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsak.getSaksnummer());
        boolean harIkkeAvsluttetKontrollbehandling = behandlinger.stream()
            .anyMatch(b -> b.erKontrollbehandling() && !b.erAvsluttet());
        return !harIkkeAvsluttetKontrollbehandling && kanRevurderingOpprettes(behandlinger);
    }

    private boolean kanRevurderingOpprettes(List<Behandling> behandlinger) {
        Optional<Behandling> gjeldendeBehandling = hentBehandlingMedVedtak(behandlinger);
        return gjeldendeBehandling.isPresent();
    }

    private Optional<Behandling> hentBehandlingMedVedtak(List<Behandling> behandlinger) {
        List<Behandling> behandlingerMedVedtak = behandlinger.stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(Behandling::erStatusFerdigbehandlet)
            .filter(behandling -> !behandling.isBehandlingHenlagt())
            .collect(Collectors.toList());
        List<Behandling> sorterteBehandlinger = behandlingerMedVedtak.stream().sorted(new BehandlingAvsluttetDatoComparator()).collect(Collectors.toList());
        return sorterteBehandlinger.isEmpty() ? Optional.empty() : Optional.of(sorterteBehandlinger.get(0));
    }

    private boolean harÅpenOrdinærBehandling(Fagsak fagsak) {
        return behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .anyMatch(Behandling::erOrdinærYtelseBehandling);
    }

    static class BehandlingAvsluttetDatoComparator implements Comparator<Behandling>, Serializable {
        @Override
        public int compare(Behandling behandling, Behandling otherBehandling) {
            return otherBehandling.getAvsluttetDato() != null && behandling.getAvsluttetDato() != null ?
                otherBehandling.getAvsluttetDato().compareTo(behandling.getAvsluttetDato())
                : otherBehandling.getOpprettetDato().compareTo(behandling.getOpprettetDato());
        }
    }
}
