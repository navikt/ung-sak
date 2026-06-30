package no.nav.ung.sak.trigger;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProsessTriggerFilterTest {

    @Test
    void skal_fjerne_varsel_opphor_ved_maksdato_nar_forlenget_periode_finnes() {
        var dato = LocalDate.now();
        var triggere = List.of(
            new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, DatoIntervallEntitet.fraOgMedTilOgMed(dato, dato)),
            new Trigger(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM, DatoIntervallEntitet.fraOgMedTilOgMed(dato.plusDays(1), dato.plusDays(10)))
        );

        var resultat = ProsessTriggerFilter.forKravperioder(triggere);

        assertThat(resultat)
            .extracting(Trigger::getÅrsak)
            .contains(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM)
            .doesNotContain(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO);
    }

    @Test
    void skal_fjerne_varsel_opphor_ved_maksdato_nar_opphor_finnes() {
        var dato = LocalDate.now();
        var triggere = List.of(
            new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, DatoIntervallEntitet.fraOgMedTilOgMed(dato, dato)),
            new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, DatoIntervallEntitet.fraOgMedTilOgMed(dato.plusDays(1), dato.plusDays(10)))
        );

        var resultat = ProsessTriggerFilter.forKravperioder(triggere);

        assertThat(resultat)
            .extracting(Trigger::getÅrsak)
            .contains(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM)
            .doesNotContain(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO);
    }

    @Test
    void skal_beholde_varsel_opphor_ved_maksdato_nar_den_ikke_er_overstyrt() {
        var dato = LocalDate.now();
        var triggere = List.of(new Trigger(
            BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO,
            DatoIntervallEntitet.fraOgMedTilOgMed(dato, dato)
        ));

        var resultat = ProsessTriggerFilter.forKravperioder(triggere);

        assertThat(resultat)
            .extracting(Trigger::getÅrsak)
            .containsExactly(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO);
    }

    @Test
    void erVarselOpphørVedMaksdatoOverstyrt_skal_vaere_true_ved_forlenget_periode() {
        assertThat(ProsessTriggerFilter.erVarselOpphørVedMaksdatoOverstyrt(List.of(
            BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO,
            BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM))).isTrue();
    }

    @Test
    void erVarselOpphørVedMaksdatoOverstyrt_skal_vaere_true_ved_opphor() {
        assertThat(ProsessTriggerFilter.erVarselOpphørVedMaksdatoOverstyrt(List.of(
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM))).isTrue();
    }

    @Test
    void erVarselOpphørVedMaksdatoOverstyrt_skal_vaere_false_for_rent_varsel_opphor() {
        assertThat(ProsessTriggerFilter.erVarselOpphørVedMaksdatoOverstyrt(List.of(
            BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO))).isFalse();
    }
}

