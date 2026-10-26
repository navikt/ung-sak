package no.nav.ung.ytelse.aktivitetspenger.registerinnhenting;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_AKTIVITETSPENGERGRUNNLAG;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.LIGNET_NÆRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AktivitetspengerInformasjonselementerUtlederTest {

    private final AktivitetspengerInformasjonselementerUtleder utleder = new AktivitetspengerInformasjonselementerUtleder();

    @Test
    void førstegangsbehandling_skal_ikke_hente_inntektsgrunnlag() {
        assertThat(utleder.utled(BehandlingType.FØRSTEGANGSSØKNAD, List.of(BehandlingÅrsakType.NY_SØKT_PERIODE)))
            .containsExactly(LIGNET_NÆRING);
    }

    @Test
    void revurdering_uten_kontroll_av_inntekt_skal_ikke_hente_inntektsgrunnlag() {
        assertThat(utleder.utled(BehandlingType.REVURDERING, List.of(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT)))
            .containsExactly(LIGNET_NÆRING);
    }

    @Test
    void revurdering_med_kontroll_av_inntekt_skal_hente_aktivitetspengergrunnlag() {
        assertThat(utleder.utled(BehandlingType.REVURDERING, List.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, BehandlingÅrsakType.NY_SØKT_PERIODE)))
            .containsExactlyInAnyOrder(INNTEKT_AKTIVITETSPENGERGRUNNLAG, LIGNET_NÆRING);
    }

    @Test
    void skal_feile_for_behandlingstype_uten_konfigurasjon() {
        assertThatThrownBy(() -> utleder.utled(BehandlingType.ANKE, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
