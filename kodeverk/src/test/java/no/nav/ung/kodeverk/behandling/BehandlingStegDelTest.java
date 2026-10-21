package no.nav.ung.kodeverk.behandling;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BehandlingStegDelTest {

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
