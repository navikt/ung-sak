package no.nav.k9.sak.ytelse.frisinn.simulering;

import static java.util.Objects.isNull;

import java.math.BigDecimal;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag.ErEndringIBeregningFRISINN;
import no.nav.k9.sak.økonomi.simulering.SimulerOppdragAksjonspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class SimulerOppdragAksjonspunktFrisinnTjeneste extends SimulerOppdragAksjonspunktTjeneste {

    @Inject
    public SimulerOppdragAksjonspunktFrisinnTjeneste() {
        //cdi lookup
    }

    public Optional<AksjonspunktDefinisjon> utledAksjonspunkt(SimuleringResultatDto simuleringResultatDto) {
        BigDecimal toleranseGrenseDagsats = ErEndringIBeregningFRISINN.TOLERANSE_GRENSE_DAGSATS;

        if (!isNull(simuleringResultatDto.getSumFeilutbetaling()) && BigDecimal.valueOf(simuleringResultatDto.getSumFeilutbetaling()).compareTo(toleranseGrenseDagsats) >= 0) {
            return Optional.of(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
        }
        if (!isNull(simuleringResultatDto.getSumInntrekk()) && simuleringResultatDto.getSumInntrekk() != 0) {
            return Optional.of(AksjonspunktDefinisjon.VURDER_INNTREKK);
        }
        return Optional.empty();
    }
}
