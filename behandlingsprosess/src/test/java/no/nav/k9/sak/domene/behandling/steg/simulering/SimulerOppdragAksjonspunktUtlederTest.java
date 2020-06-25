package no.nav.k9.sak.domene.behandling.steg.simulering;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.sak.økonomi.simulering.SimulerOppdragAksjonspunktTjeneste;

public class SimulerOppdragAksjonspunktUtlederTest {


    @Test
    public void skal_gi_aksjonspunkt_for_feilutbetaling_uten_mulighet_for_inntrekk_når_det_finnes_feilutbetaling() {
        assertThat(finnAksjonspunkt(-1, 0)).contains(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
    }

    @Test
    public void skal_gi_aksjonspunkt_for_feilutbetaling_uten_mulighet_for_inntrekk_når_det_inntrekk_men_det_fortsatt_er_restfeilutbetaling() {
        assertThat(finnAksjonspunkt(-100, -1)).contains(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
    }

    @Test
    public void skal_gi_aksjonspunkt_for_feilutbetaling_med_mulighet_for_inntrekk_når_det_finnes_inntrekk_og_ikke_restfeilutbetaling() {
        assertThat(finnAksjonspunkt(0, -1)).contains(AksjonspunktDefinisjon.VURDER_INNTREKK);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_når_feilutbetaling_og_inntrekk_er_0() {
        assertThat(finnAksjonspunkt(0, 0)).isEmpty();
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_hvis_beløp_er_null() {
        assertThat(SimulerOppdragAksjonspunktTjeneste.utledAksjonspunkt(new SimuleringResultatDto(null, null, false))).isEmpty();
    }

    private Optional<AksjonspunktDefinisjon> finnAksjonspunkt(int feilutbetalt, int inntrekk) {
        return SimulerOppdragAksjonspunktTjeneste.utledAksjonspunkt(new SimuleringResultatDto((long) feilutbetalt, (long) inntrekk, false));
    }

}
