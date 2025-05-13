package no.nav.ung.sak.domene.behandling.steg.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;

import java.util.Optional;

@Dependent
// TODO: Denne kan fjernes
public class VentPåRelevantVedtakUtleder {

    private BehandlingRepository behandlingRepository;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;

    @Inject
    public VentPåRelevantVedtakUtleder(BehandlingRepository behandlingRepository, ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
    }

    public Optional<AksjonspunktDefinisjon> utledVentepunktForRelevantVedtak(Behandling kontrollbehandling) {
        final var sisteYtelsesBehandlingForFagsakId = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(kontrollbehandling.getFagsakId());
        if (sisteYtelsesBehandlingForFagsakId.isPresent() && !sisteYtelsesBehandlingForFagsakId.get().getStatus().erFerdigbehandletStatus()) {
            final var revelantBehandling = sisteYtelsesBehandlingForFagsakId.get();
            final var kontrollTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(kontrollbehandling.getId());
            final var relevantBehandlingTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(revelantBehandling.getId());
            if (kontrollTidslinje.intersection(relevantBehandlingTidslinje).isEmpty()) {
                return Optional.of(AksjonspunktDefinisjon.AUTO_SATT_PÅ_RELEVANT_BEHANDLING);
            }
        }
        return Optional.empty();
    }

}
