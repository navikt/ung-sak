package no.nav.k9.sak.ytelse.frisinn.simulering;

import static java.util.Objects.isNull;

import java.math.BigDecimal;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag.ErEndringIBeregningRettsgebyrFRISINN;
import no.nav.k9.sak.økonomi.simulering.SimulerOppdragAksjonspunktTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class SimulerOppdragAksjonspunktFrisinnTjeneste extends SimulerOppdragAksjonspunktTjeneste {
    private Boolean ignorerUgunstOpptillRettsgebyr;

    public SimulerOppdragAksjonspunktFrisinnTjeneste() {
        // CDI
    }

    @Inject
    public SimulerOppdragAksjonspunktFrisinnTjeneste(@KonfigVerdi(value = "KAN_HA_UGUNST_OPPTIL_RETTSGEBYR", defaultVerdi = "false") Boolean ugunstMedFeiltoleranse) {
        this.ignorerUgunstOpptillRettsgebyr = ugunstMedFeiltoleranse;
    }

    @Override
    public Optional<AksjonspunktDefinisjon> utledAksjonspunkt(SimuleringResultatDto simuleringResultatDto) {
        BigDecimal toleranseGrenseDagsats = ErEndringIBeregningRettsgebyrFRISINN.TOLERANSE_GRENSE_DAGSATS;

        if (ignorerUgunstOpptillRettsgebyr) {
            if (!isNull(simuleringResultatDto.getSumFeilutbetaling()) && BigDecimal.valueOf(simuleringResultatDto.getSumFeilutbetaling()).compareTo(toleranseGrenseDagsats) >= 0) {
                return Optional.of(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
            }
        } else {
            if (!isNull(simuleringResultatDto.getSumFeilutbetaling()) && simuleringResultatDto.getSumFeilutbetaling() != 0) {
                return Optional.of(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
            }
        }
        if (!isNull(simuleringResultatDto.getSumInntrekk()) && simuleringResultatDto.getSumInntrekk() != 0) {
            return Optional.of(AksjonspunktDefinisjon.VURDER_INNTREKK);
        }
        return Optional.empty();
    }
}
