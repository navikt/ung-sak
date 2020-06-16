package no.nav.k9.sak.økonomi.simulering;

import static java.util.Objects.isNull;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@FagsakYtelseTypeRef("*")
public class SimulerOppdragAksjonspunktTjeneste {

    @Inject
    public SimulerOppdragAksjonspunktTjeneste() {
        //Skal ikke instansieres
    }


    public Optional<AksjonspunktDefinisjon> utledAksjonspunkt(SimuleringResultatDto simuleringResultatDto) {
        if (!isNull(simuleringResultatDto.getSumFeilutbetaling()) && simuleringResultatDto.getSumFeilutbetaling() != 0) {
            return Optional.of(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
        }
        if (!isNull(simuleringResultatDto.getSumInntrekk()) && simuleringResultatDto.getSumInntrekk() != 0) {
            return Optional.of(AksjonspunktDefinisjon.VURDER_INNTREKK);
        }
        return Optional.empty();
    }
}
