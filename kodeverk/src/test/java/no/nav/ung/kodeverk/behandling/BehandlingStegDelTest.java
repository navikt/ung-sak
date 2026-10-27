package no.nav.ung.kodeverk.behandling;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BehandlingStegDelTest {

    @Test
    void nav_kontor_steg_skal_gi_nav_kontor() {
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.NAV_KONTOR_FORESLÅ_VILKÅR)).isEqualTo(BehandlingDel.NAV_KONTOR);
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.VURDER_BOSTEDVILKÅR)).isEqualTo(BehandlingDel.NAV_KONTOR);
    }

    @Test
    void sentrale_steg_skal_gi_sentral() {
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.START_STEG)).isEqualTo(BehandlingDel.SENTRAL);
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.FORESLÅ_VEDTAK)).isEqualTo(BehandlingDel.SENTRAL);
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.FATTE_VEDTAK)).isEqualTo(BehandlingDel.SENTRAL);
    }

    @Test
    void klagesteg_skal_gi_sentral() {
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.VURDER_FORMKRAV_KLAGE_FØRSTEINSTANS)).isEqualTo(BehandlingDel.SENTRAL);
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.VURDER_KLAGE_FØRSTEINSTANS)).isEqualTo(BehandlingDel.SENTRAL);
        assertThat(BehandlingStegDel.forAktivitetspenger(BehandlingStegType.OVERFØRT_NK)).isEqualTo(BehandlingDel.SENTRAL);
    }

    @Test
    void utled_gir_tom_for_andre_ytelser_og_uten_aktivt_steg() {
        assertThat(BehandlingStegDel.utled(FagsakYtelseType.UNGDOMSYTELSE, BehandlingStegType.FORESLÅ_VEDTAK)).isEmpty();
        assertThat(BehandlingStegDel.utled(FagsakYtelseType.AKTIVITETSPENGER, null)).isEmpty();
        assertThat(BehandlingStegDel.utled(FagsakYtelseType.AKTIVITETSPENGER, BehandlingStegType.VURDER_BOSTEDVILKÅR)).contains(BehandlingDel.NAV_KONTOR);
    }

    @Test
    void steg_som_ikke_inngår_i_aktivitetspenger_skal_feile() {
        assertThatThrownBy(() -> BehandlingStegDel.forAktivitetspenger(BehandlingStegType.UNGDOMSYTELSE_BEREGNING))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aksjonspunkt_og_steget_det_ligger_på_skal_ha_samme_del() {
        for (AksjonspunktDefinisjon aksjonspunkt : AksjonspunktDefinisjon.values()) {
            // Autopunkt løses ikke av saksbehandler, og typen sier derfor ikke noe om del.
            if (aksjonspunkt.getBehandlingSteg() == null || aksjonspunkt.erAutopunkt()) {
                continue;
            }
            BehandlingDel del;
            try {
                del = BehandlingStegDel.forAktivitetspenger(aksjonspunkt.getBehandlingSteg());
            } catch (IllegalArgumentException e) {
                //ignorerer de som feiler da de gjelder ikke aktivitetspenger.
                continue;
            }
            assertThat(del)
                .as("Del for steget %s til aksjonspunkt %s", aksjonspunkt.getBehandlingSteg(), aksjonspunkt.getKode())
                .isEqualTo(aksjonspunkt.getBehandlingDel());
        }
    }
}
