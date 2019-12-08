package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;

@Dependent
public class KøKontroller {

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingRepository behandlingRepository;

    public KøKontroller() {
        // For CDI proxy
    }

    @Inject
    public KøKontroller(BehandlingProsesseringTjeneste prosesseringTjeneste,
                        BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                        BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingProsesseringTjeneste = prosesseringTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    void dekøFørsteBehandlingISakskompleks(Behandling behandling) {
        opprettTaskForÅStarteBehandling(behandling);
    }

    public void enkøBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        BehandlingÅrsak.builder(BehandlingÅrsakType.KØET_BEHANDLING).buildFor(behandling);
        behandlingRepository.lagre(behandling, lås);
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, null, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
    }

    private void opprettTaskForÅStarteBehandling(Behandling behandling) {
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(behandling);
    }
}
