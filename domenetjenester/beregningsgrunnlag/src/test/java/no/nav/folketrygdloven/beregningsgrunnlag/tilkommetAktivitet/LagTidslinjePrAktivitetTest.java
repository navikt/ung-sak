package no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.response.v1.Arbeidsgiver;
import no.nav.folketrygdloven.kalkulus.response.v1.tilkommetAktivitet.UtledetTilkommetAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class LagTidslinjePrAktivitetTest {

    @Test
    void skal_ikke_finne_ny_aktivitet_dersom_filtreringsperioder_er_tom() {

        var uuid = UUID.randomUUID();
        var tilkommetFom = LocalDate.now();
        var tilkommetTom = LocalDate.now().plusDays(10);
        var aktivitetMap = Map.of(uuid, List.of(lagTilkommetAktivitet(tilkommetFom, tilkommetTom)));
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5));

        var resultat = LagTidslinjePrAktivitet.lagTidslinjePrNyAktivitet(new TreeSet<>(), aktivitetMap, Map.of(uuid, vilkårsperiode));

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_finne_ny_aktivitet() {

        var uuid = UUID.randomUUID();
        var tilkommetFom = LocalDate.now();
        var tilkommetTom = LocalDate.now().plusDays(10);
        var aktivitetMap = Map.of(uuid, List.of(lagTilkommetAktivitet(tilkommetFom, tilkommetTom)));
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5));

        var resultat = LagTidslinjePrAktivitet.lagTidslinjePrNyAktivitet(new TreeSet<>(Set.of(vilkårsperiode)), aktivitetMap, Map.of(uuid, vilkårsperiode));

        assertThat(resultat.isEmpty()).isFalse();
        var entries = resultat.entrySet();
        assertThat(entries.size()).isEqualTo(1);
        var entry = entries.iterator().next();
        var intervaller = entry.getValue().getLocalDateIntervals();
        assertThat(intervaller.size()).isEqualTo(1);
        assertThat(intervaller.getFirst().getFomDato()).isEqualTo(tilkommetFom);
        assertThat(intervaller.getFirst().getTomDato()).isEqualTo(vilkårsperiode.getTomDato());
    }

    @Test
    void skal_kun_finne_aktivitet_for_vilkårsperioden_der_denne_er_tilkommet() {

        var uuid1 = UUID.randomUUID();
        var uuid2 = UUID.randomUUID();
        var tilkommetFom = LocalDate.now();
        var tilkommetTom = LocalDate.now().plusDays(10);
        var aktivitetMap = Map.of(uuid1, List.of(lagTilkommetAktivitet(tilkommetFom, tilkommetTom)));
        var vilkårsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5));
        var vilkårsperiode2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusDays(20), LocalDate.now().plusDays(25));
        var vilkårsperiodeMap = Map.of(uuid1, vilkårsperiode1, uuid2, vilkårsperiode2);
        var resultat = LagTidslinjePrAktivitet.lagTidslinjePrNyAktivitet(new TreeSet<>(Set.of(vilkårsperiode1, vilkårsperiode2)), aktivitetMap, vilkårsperiodeMap);

        assertThat(resultat.isEmpty()).isFalse();
        var entries = resultat.entrySet();
        assertThat(entries.size()).isEqualTo(1);
        var entry = entries.iterator().next();
        var intervaller = entry.getValue().getLocalDateIntervals();
        assertThat(intervaller.size()).isEqualTo(1);
        assertThat(intervaller.getFirst().getFomDato()).isEqualTo(tilkommetFom);
        assertThat(intervaller.getFirst().getTomDato()).isEqualTo(vilkårsperiode1.getTomDato());
    }

    private static UtledetTilkommetAktivitet lagTilkommetAktivitet(LocalDate fom, LocalDate tom) {
        return new UtledetTilkommetAktivitet(AktivitetStatus.ARBEIDSTAKER, new Arbeidsgiver("123456789", null), List.of(new Periode(fom, tom)));
    }
}
