package no.nav.ung.kodeverk.behandling;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BehandlingStegDelTest {

    @Test
    void nav_kontor_steg_skal_gi_nav_kontor() {
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.NAV_KONTOR_FORESLÅ_VILKÅR)).contains(BehandlingDel.NAV_KONTOR);
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.VURDER_BOSTEDVILKÅR)).contains(BehandlingDel.NAV_KONTOR);
    }

    @Test
    void sentrale_steg_skal_gi_sentral() {
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.START_STEG)).contains(BehandlingDel.SENTRAL);
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.FORESLÅ_VEDTAK)).contains(BehandlingDel.SENTRAL);
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.FATTE_VEDTAK)).contains(BehandlingDel.SENTRAL);
    }

    @Test
    void steg_som_ikke_inngår_i_aktivitetspenger_skal_ikke_ha_del() {
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.UNGDOMSYTELSE_BEREGNING)).isEmpty();
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.VURDER_KLAGE_FØRSTEINSTANS)).isEmpty();
    }

    @Test
    void aksjonspunkt_og_steget_det_ligger_på_skal_ha_samme_del() {
        for (AksjonspunktDefinisjon aksjonspunkt : AksjonspunktDefinisjon.values()) {
            if (aksjonspunkt.getBehandlingSteg() == null) {
                continue;
            }
            BehandlingStegDel.forAktivitetspenger(aksjonspunkt.getBehandlingSteg())
                .ifPresent(del -> assertThat(del)
                    .as("Del for steget %s til aksjonspunkt %s", aksjonspunkt.getBehandlingSteg(), aksjonspunkt.getKode())
                    .isEqualTo(aksjonspunkt.getBehandlingDel()));
        }
    }
}
