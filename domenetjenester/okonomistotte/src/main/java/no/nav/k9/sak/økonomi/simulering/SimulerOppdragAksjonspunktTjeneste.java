package no.nav.k9.sak.økonomi.simulering;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;

import java.util.Optional;

import static java.util.Objects.isNull;

public class SimulerOppdragAksjonspunktTjeneste {

    private SimulerOppdragAksjonspunktTjeneste() {
        // Skjuler default konstruktør
    }

    public static Optional<AksjonspunktDefinisjon> utledAksjonspunkt(SimuleringResultatDto simuleringResultatDto) {
        if (!isNull(simuleringResultatDto.getSumFeilutbetaling()) && simuleringResultatDto.getSumFeilutbetaling() != 0) {
            return Optional.of(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
        }
        if (!isNull(simuleringResultatDto.getSumInntrekk()) && simuleringResultatDto.getSumInntrekk() != 0) {
            return Optional.of(AksjonspunktDefinisjon.VURDER_INNTREKK);
        }
        return Optional.empty();
    }
}
