package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.typer.Arbeidsgiver;

class EtterlysInntektsmeldingUtlederTest {

    private EtterlysInntektsmeldingUtleder utleder = new EtterlysInntektsmeldingUtleder();

    @Test
    void skal_fortsette_hvis_komplett() {
        var aksjonspunkter = Map.of(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING, LocalDateTime.now().plusDays(10));
        var input = new EtterlysningInput(aksjonspunkter, Map.of(), Map.of());

        var aksjon = utleder.utled(input);


        assertThat(aksjon).isNotNull();
        assertThat(aksjon.kanFortsette()).isTrue();
    }

    @Test
    void skal_fortsatt_gå_på_vent_hvis_aksjonspunkt() {
        var aksjonspunkter = Map.of(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING, LocalDateTime.now().plusDays(10));
        var input = new EtterlysningInput(aksjonspunkter,
            Map.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), List.of(new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, Arbeidsgiver.virksomhet("000000000")))),
            Map.of());

        var aksjon = utleder.utled(input);

        assertThat(aksjon).isNotNull();
        assertThat(aksjon.kanFortsette()).isFalse();
        assertThat(aksjon.getFrist()).isNotNull();
        assertThat(aksjon.getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING);
    }

    @Test
    void skal_gi_fortsett_hvis_frist_utløpt_og_ikke_komplett() {
        var aksjonspunkter = Map.of(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING, LocalDateTime.now().minusDays(10));
        var input = new EtterlysningInput(aksjonspunkter,
            Map.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), List.of(new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, Arbeidsgiver.virksomhet("000000000")))),
            Map.of());

        var aksjon = utleder.utled(input);

        assertThat(aksjon).isNotNull();
        assertThat(aksjon.kanFortsette()).isFalse();
        assertThat(aksjon.erUavklart()).isTrue();
    }
}
