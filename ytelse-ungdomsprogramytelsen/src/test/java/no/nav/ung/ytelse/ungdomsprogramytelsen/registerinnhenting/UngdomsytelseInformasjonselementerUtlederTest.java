package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinnhenting;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_UNGDOMSYTELSEGRUNNLAG;
import static org.assertj.core.api.Assertions.assertThat;

class UngdomsytelseInformasjonselementerUtlederTest {

    private final UngdomsytelseInformasjonselementerUtleder utleder = new UngdomsytelseInformasjonselementerUtleder();

    @Test
    void førstegangsbehandling_skal_ikke_hente_registerdata() {
        assertThat(utleder.utled(BehandlingType.FØRSTEGANGSSØKNAD, List.of(BehandlingÅrsakType.NY_SØKT_PERIODE))).isEmpty();
    }

    @Test
    void revurdering_med_kontroll_av_inntekt_skal_hente_ungdomsytelsegrunnlag() {
        assertThat(utleder.utled(BehandlingType.REVURDERING, List.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)))
            .containsExactly(INNTEKT_UNGDOMSYTELSEGRUNNLAG);
    }
}
