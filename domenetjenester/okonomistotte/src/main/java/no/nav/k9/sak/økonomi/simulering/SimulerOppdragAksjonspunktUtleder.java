package no.nav.k9.sak.økonomi.simulering;

import static java.util.Objects.isNull;

import java.util.Optional;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;

public class SimulerOppdragAksjonspunktUtleder {

    private SimulerOppdragAksjonspunktUtleder() {
        //Skal ikke instansieres
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
