package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;

/**
 * Kun for invortes bruk (Behandlingskontroll). Evt. tester. Skal ikke aksesseres direkte av andre under normal
 * operasjon.
 */
@ApplicationScoped
public class InternalManipulerBehandling {

    @Inject
    public InternalManipulerBehandling() {
    }

    /**
     * Sett til angitt steg, default steg status.
     */
    public void forceOppdaterBehandlingSteg(Behandling behandling, BehandlingStegType stegType) {
        forceOppdaterBehandlingSteg(behandling, stegType, BehandlingStegStatus.UDEFINERT, BehandlingStegStatus.UTFØRT);
    }

    /**
     * Sett Behandling til angitt steg, angitt steg status, default slutt status for andre åpne steg.
     */
    public void forceOppdaterBehandlingSteg(Behandling behandling, BehandlingStegType stegType, BehandlingStegStatus stegStatus) {
        forceOppdaterBehandlingSteg(behandling, stegType, stegStatus, BehandlingStegStatus.UTFØRT);
    }

    /**
     * Sett Behandling til angitt steg, angitt steg status, angitt slutt status for andre åpne steg.
     */
    public void forceOppdaterBehandlingSteg(Behandling behandling, BehandlingStegType stegType, BehandlingStegStatus nesteStegStatus,
                                            BehandlingStegStatus ikkeFerdigStegStatus) {

        // finn riktig mapping av kodeverk slik at vi får med dette når Behandling brukes videre.
        Optional<BehandlingStegTilstand> eksisterendeTilstand = behandling.getSisteBehandlingStegTilstand();
        if (!eksisterendeTilstand.isPresent() || erUlikeSteg(stegType, eksisterendeTilstand)) {
            if (eksisterendeTilstand.isPresent() && !BehandlingStegStatus.erSluttStatus(eksisterendeTilstand.get().getBehandlingStegStatus())) {
                if (!BehandlingStegStatus.erSluttStatus(ikkeFerdigStegStatus)) {
                    throw new IllegalStateException("Tidligere steg må avsluttes riktig, fikk " + ikkeFerdigStegStatus + "for " + eksisterendeTilstand
                        + " på behandling " + behandling + ". Neste steg " + stegType + ". Neste stegStatus " + nesteStegStatus);
                }
                eksisterendeTilstand.ifPresent(it -> it.setBehandlingStegStatus(ikkeFerdigStegStatus));
            }
            BehandlingStegTilstand tilstand = new BehandlingStegTilstand(behandling, stegType);
            tilstand.setBehandlingStegStatus(nesteStegStatus);
            behandling.oppdaterBehandlingStegOgStatus(tilstand);
        } else {
            eksisterendeTilstand.ifPresent(it -> it.setBehandlingStegStatus(nesteStegStatus));
        }
    }

    private boolean erUlikeSteg(BehandlingStegType stegType, Optional<BehandlingStegTilstand> eksisterendeTilstand) {
        return !eksisterendeTilstand.get().getBehandlingSteg().equals(stegType);
    }

}
