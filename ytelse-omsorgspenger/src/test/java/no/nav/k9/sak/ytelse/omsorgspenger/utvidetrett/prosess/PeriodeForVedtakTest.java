package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import static no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.UtvidetRettIverksettTask.periodeForVedtak;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.k9.sak.typer.Periode;

public class PeriodeForVedtakTest {

    @Test
    public void en_innvilget_periode() {
        var periode = new Periode("2021-01-01/2021-12-31");

        var samletVilkårsresultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode.getFom(), periode.getTom(), VilkårUtfallSamlet.fra(List.of(oppfylt())))
        ));

        assertEquals(periode, periodeForVedtak(samletVilkårsresultat, true));
        var exception = assertThrows(IllegalStateException.class, () -> periodeForVedtak(samletVilkårsresultat, false));
        assertEquals(
            "Uventet samlet vilkårsresultat. Innvilget=[false], OppfyltePerioder=[Periode<fom=2021-01-01, tom=2021-12-31>], IkkeOppfyltePerioder=[]; samletVilkårResultat=LocalDateTimeline<2021-01-01, 2021-12-31 [1]> = [[2021-01-01, 2021-12-31]]",
            exception.getMessage());
    }

    @Test
    public void en_avslått_periode() {
        var periode = new Periode("2021-01-01/2021-12-31");

        var samletVilkårsresultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode.getFom(), periode.getTom(), VilkårUtfallSamlet.fra(List.of(ikkeOppfylt())))
        ));

        assertEquals(periode, periodeForVedtak(samletVilkårsresultat, false));
        var exception = assertThrows(IllegalStateException.class, () -> periodeForVedtak(samletVilkårsresultat, true));
        assertEquals(
            "Uventet samlet vilkårsresultat. Innvilget=[true], OppfyltePerioder=[], IkkeOppfyltePerioder=[Periode<fom=2021-01-01, tom=2021-12-31>]; samletVilkårResultat=LocalDateTimeline<2021-01-01, 2021-12-31 [1]> = [[2021-01-01, 2021-12-31]]",
            exception.getMessage());
    }

    @Test
    public void en_innvilget_og_en_avslått_periode() {
        var innvilgetPeriode = new Periode("2021-01-01/2021-12-31");
        var avslåttPeriode = new Periode("2022-01-01/2022-12-31");

        var samletVilkårsresultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(innvilgetPeriode.getFom(), innvilgetPeriode.getTom(), VilkårUtfallSamlet.fra(List.of(oppfylt()))),
            new LocalDateSegment<>(avslåttPeriode.getFom(), avslåttPeriode.getTom(), VilkårUtfallSamlet.fra(List.of(ikkeOppfylt())))
        ));

        assertEquals(innvilgetPeriode, periodeForVedtak(samletVilkårsresultat, true));
        var exception = assertThrows(IllegalStateException.class, () -> periodeForVedtak(samletVilkårsresultat, false));
        assertEquals(
            "Uventet samlet vilkårsresultat. Innvilget=[false], OppfyltePerioder=[Periode<fom=2021-01-01, tom=2021-12-31>], IkkeOppfyltePerioder=[Periode<fom=2022-01-01, tom=2022-12-31>]; samletVilkårResultat=LocalDateTimeline<2021-01-01, 2022-12-31 [2]> = [[2021-01-01, 2021-12-31],[2022-01-01, 2022-12-31]]",
            exception.getMessage());
    }

    @Test
    public void ugyldig_vedtaksperiode() {
        var periode = new Periode(Tid.TIDENES_BEGYNNELSE, LocalDate.parse("2021-04-09"));

        var samletVilkårsresultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode.getFom(), periode.getTom(), VilkårUtfallSamlet.fra(List.of(oppfylt())))
        ));

        var exception = assertThrows(IllegalStateException.class, () -> periodeForVedtak(samletVilkårsresultat, true));
        assertEquals("Ugyldig periode for vedtak om utvidet rett. fom=-4712-01-01, tom=2021-04-09", exception.getMessage());
    }

    @Test
    public void flere_innvilgede_perioder() {
        var periode1 = new Periode("2021-01-01/2021-12-31");
        var periode2 = new Periode("2022-05-01/2022-12-31");

        var samletVilkårsresultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode1.getFom(), periode1.getTom(), VilkårUtfallSamlet.fra(List.of(oppfylt()))),
            new LocalDateSegment<>(periode2.getFom(), periode2.getTom(), VilkårUtfallSamlet.fra(List.of(oppfylt())))
        ));

        var exception = assertThrows(IllegalStateException.class, () -> periodeForVedtak(samletVilkårsresultat, true));
        assertEquals(
            "Uventet samlet vilkårsresultat. Innvilget=[true], OppfyltePerioder=[Periode<fom=2021-01-01, tom=2021-12-31>, Periode<fom=2022-05-01, tom=2022-12-31>], IkkeOppfyltePerioder=[]; samletVilkårResultat=LocalDateTimeline<2021-01-01, 2022-12-31 [2]> = [[2021-01-01, 2021-12-31],[2022-05-01, 2022-12-31]]",
            exception.getMessage());
        exception = assertThrows(IllegalStateException.class, () -> periodeForVedtak(samletVilkårsresultat, false));
        assertEquals(
            "Uventet samlet vilkårsresultat. Innvilget=[false], OppfyltePerioder=[Periode<fom=2021-01-01, tom=2021-12-31>, Periode<fom=2022-05-01, tom=2022-12-31>], IkkeOppfyltePerioder=[]; samletVilkårResultat=LocalDateTimeline<2021-01-01, 2022-12-31 [2]> = [[2021-01-01, 2021-12-31],[2022-05-01, 2022-12-31]]",
            exception.getMessage());

    }

    @Test
    public void flere_avslåtte_perioder() {
        var periode1 = new Periode("2021-01-01/2021-12-31");
        var periode2 = new Periode("2022-05-01/2022-12-31");

        var samletVilkårsresultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode1.getFom(), periode1.getTom(), VilkårUtfallSamlet.fra(List.of(ikkeOppfylt()))),
            new LocalDateSegment<>(periode2.getFom(), periode2.getTom(), VilkårUtfallSamlet.fra(List.of(ikkeOppfylt())))
        ));

        var exception = assertThrows(IllegalStateException.class, () -> periodeForVedtak(samletVilkårsresultat, true));
        assertEquals(
            "Uventet samlet vilkårsresultat. Innvilget=[true], OppfyltePerioder=[], IkkeOppfyltePerioder=[Periode<fom=2021-01-01, tom=2021-12-31>, Periode<fom=2022-05-01, tom=2022-12-31>]; samletVilkårResultat=LocalDateTimeline<2021-01-01, 2022-12-31 [2]> = [[2021-01-01, 2021-12-31],[2022-05-01, 2022-12-31]]",
            exception.getMessage());
        exception = assertThrows(IllegalStateException.class, () -> periodeForVedtak(samletVilkårsresultat, false));
        assertEquals(
            "Uventet samlet vilkårsresultat. Innvilget=[false], OppfyltePerioder=[], IkkeOppfyltePerioder=[Periode<fom=2021-01-01, tom=2021-12-31>, Periode<fom=2022-05-01, tom=2022-12-31>]; samletVilkårResultat=LocalDateTimeline<2021-01-01, 2022-12-31 [2]> = [[2021-01-01, 2021-12-31],[2022-05-01, 2022-12-31]]",
            exception.getMessage());
    }

    private static VilkårUtfallSamlet.VilkårUtfall oppfylt() {
        return new VilkårUtfallSamlet.VilkårUtfall(VilkårType.UTVIDETRETT, null, Utfall.OPPFYLT);
    }
    private static VilkårUtfallSamlet.VilkårUtfall ikkeOppfylt() {
        return new VilkårUtfallSamlet.VilkårUtfall(VilkårType.UTVIDETRETT, Avslagsårsak.IKKE_UTVIDETRETT_IKKE_KRONISK_SYK, Utfall.IKKE_OPPFYLT);
    }
}
