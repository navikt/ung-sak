package no.nav.ung.ytelse.aktivitetspenger.historikkinnslag;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinje;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
class VilkårsvurderingHistorikkinnslagTjenesteTest {

    long behanldingId = 2;
    long fagsakId = 1;

    @Inject
    VilkårsvurderingHistorikkinnslagTjeneste tjeneste;

    @Test
    void skal_lage_historikkinnslag_når_saksbehandler_innvilger_første_gang() {
        LocalDate innvilgetFom = LocalDate.of(2026, 9, 11);
        LocalDate innvilgetTom = innvilgetFom.plusWeeks(52).minusDays(1);

        LocalDateTimeline<HistorikkinnslagData> vilkårtidslinje = new LocalDateTimeline<>(innvilgetFom, innvilgetTom, HistorikkinnslagData.oppfylt());

        HistorikkinnslagInput input = new HistorikkinnslagInput()
            .setVilkårType(VilkårType.BOSTEDSVILKÅR)
            .setBehandlingId(behanldingId)
            .setEksisterendeVurderinger(LocalDateTimeline.empty())
            .setGjelderOpphør(false)
            .setNyeVurderinger(vilkårtidslinje)
            .setHistorikkAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .setSaksbehandlerIdent("Z000000");
        List<Historikkinnslag> historikkinnslag = tjeneste.lagHistorikkinnslag(fagsakId, input);
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getLinjer()).containsOnly(
            HistorikkinnslagLinje.tekst("Perioden 11.09.2026 - 09.09.2027 ble vurdert til Oppfylt.", 0)
        );
    }

    @Test
    void skal_lage_historikkinnslag_når_saksbehandler_innvilger_korter_enn_260_dager_første_gang() {
        LocalDate innvilgetFom = LocalDate.of(2026, 9, 11);
        LocalDate innvilgetTom = innvilgetFom.plusWeeks(4).minusDays(1);

        LocalDateTimeline<HistorikkinnslagData> vilkårtidslinje = new LocalDateTimeline<>(innvilgetFom, innvilgetTom, HistorikkinnslagData.oppfylt());

        HistorikkinnslagInput input = new HistorikkinnslagInput()
            .setVilkårType(VilkårType.BOSTEDSVILKÅR)
            .setBehandlingId(behanldingId)
            .setEksisterendeVurderinger(LocalDateTimeline.empty())
            .setGjelderOpphør(false)
            .setNyeVurderinger(vilkårtidslinje)
            .setHistorikkAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .setSaksbehandlerIdent("Z000000");
        List<Historikkinnslag> historikkinnslag = tjeneste.lagHistorikkinnslag(fagsakId, input);
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getLinjer()).containsOnly(
            HistorikkinnslagLinje.tekst("Perioden 11.09.2026 - 08.10.2026 ble vurdert til Oppfylt.", 0)
        );
    }

    @Test
    void skal_lage_historikkinnslag_når_saksbehandler_justerer_innvilgelse_til_en_litt_lenger_periode() {
        LocalDate innvilgetFom = LocalDate.of(2026, 9, 11);
        LocalDate innvilgetTom1 = innvilgetFom.plusWeeks(4).minusDays(1);
        LocalDate innvilgetTom2 = innvilgetTom1.plusDays(7);

        LocalDateTimeline<HistorikkinnslagData> vilkårtidslinjeFør = new LocalDateTimeline<>(innvilgetFom, innvilgetTom1, HistorikkinnslagData.oppfylt());
        LocalDateTimeline<HistorikkinnslagData> vilkårtidslinjeNå = new LocalDateTimeline<>(innvilgetFom, innvilgetTom2, HistorikkinnslagData.oppfylt());

        HistorikkinnslagInput input = new HistorikkinnslagInput()
            .setVilkårType(VilkårType.BOSTEDSVILKÅR)
            .setBehandlingId(behanldingId)
            .setEksisterendeVurderinger(vilkårtidslinjeFør)
            .setGjelderOpphør(false)
            .setNyeVurderinger(vilkårtidslinjeNå)
            .setHistorikkAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .setSaksbehandlerIdent("Z000000");
        List<Historikkinnslag> historikkinnslag = tjeneste.lagHistorikkinnslag(fagsakId, input);
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getLinjer()).containsOnly(
            HistorikkinnslagLinje.tekst("Perioden 09.10.2026 - 15.10.2026 ble vurdert til Oppfylt.", 0)
        );
    }

    @Test
    void skal_lage_historikkinnslag_når_saksbehandler_opphører_fra_start() {
        LocalDate innvilgetFom = LocalDate.of(2026, 9, 11);
        LocalDate innvilgetTom = innvilgetFom.plusWeeks(52).minusDays(1);

        LocalDateTimeline<HistorikkinnslagData> opprinneligInnvilgetTidslinje = new LocalDateTimeline<>(innvilgetFom, innvilgetTom, HistorikkinnslagData.oppfylt());
        LocalDateTimeline<HistorikkinnslagData> nyVilkårTidslinje = opprinneligInnvilgetTidslinje.mapValue(_-> HistorikkinnslagData.avslått(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED));

        HistorikkinnslagInput input = new HistorikkinnslagInput()
            .setVilkårType(VilkårType.BOSTEDSVILKÅR)
            .setBehandlingId(behanldingId)
            .setEksisterendeVurderinger(opprinneligInnvilgetTidslinje)
            .setGjelderOpphør(true)
            .setNyeVurderinger(nyVilkårTidslinje)
            .setHistorikkAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .setSaksbehandlerIdent("Z000000");
        List<Historikkinnslag> historikkinnslag = tjeneste.lagHistorikkinnslag(fagsakId, input);
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getLinjer()).containsOnly(
            HistorikkinnslagLinje.tekst("Opphørsdato satt til 11.09.2026. Søker bor et sted som ikke er forenelig med ytelsen.", 0)
        );
    }

    @Test
    void skal_lage_historikkinnslag_når_saksbehandler_opphører_fra_en_dato() {
        LocalDate innvilgetFom = LocalDate.of(2026, 9, 11);
        LocalDate innvilgetTom = innvilgetFom.plusWeeks(52).minusDays(1);
        LocalDate opphørFom = LocalDate.of(2026, 9, 30);

        LocalDateTimeline<HistorikkinnslagData> opprinneligInnvilgetTidslinje = new LocalDateTimeline<>(innvilgetFom, innvilgetTom, HistorikkinnslagData.oppfylt());
        LocalDateTimeline<HistorikkinnslagData> opphør = new LocalDateTimeline<>(opphørFom, innvilgetTom, HistorikkinnslagData.avslått(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED));
        LocalDateTimeline<HistorikkinnslagData> nyVilkårTidslinje = opprinneligInnvilgetTidslinje.crossJoin(opphør, StandardCombinators::coalesceRightHandSide);

        HistorikkinnslagInput input = new HistorikkinnslagInput()
            .setVilkårType(VilkårType.BOSTEDSVILKÅR)
            .setBehandlingId(behanldingId)
            .setEksisterendeVurderinger(opprinneligInnvilgetTidslinje)
            .setGjelderOpphør(true)
            .setNyeVurderinger(nyVilkårTidslinje)
            .setHistorikkAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .setSaksbehandlerIdent("Z000000");
        List<Historikkinnslag> historikkinnslag = tjeneste.lagHistorikkinnslag(fagsakId, input);
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getLinjer()).containsOnly(
            HistorikkinnslagLinje.tekst("Opphørsdato satt til 30.09.2026. Søker bor et sted som ikke er forenelig med ytelsen.", 0)
        );
    }

    @Test
    void skal_lage_historikkinnslag_når_saksbehandler_lar_være_å_sette_opphørsdato_i_en_opphørsbehandling() {
        LocalDate innvilgetFom = LocalDate.of(2026, 9, 11);
        LocalDate innvilgetTom = innvilgetFom.plusWeeks(52).minusDays(1);
        LocalDate opphørFom = LocalDate.of(2026, 9, 30);

        LocalDateTimeline<HistorikkinnslagData> opprinneligInnvilgetTidslinje = new LocalDateTimeline<>(innvilgetFom, innvilgetTom, HistorikkinnslagData.oppfylt());
        LocalDateTimeline<HistorikkinnslagData> nyVilkårTidslinje = opprinneligInnvilgetTidslinje;

        HistorikkinnslagInput input = new HistorikkinnslagInput()
            .setVilkårType(VilkårType.BOSTEDSVILKÅR)
            .setBehandlingId(behanldingId)
            .setEksisterendeVurderinger(opprinneligInnvilgetTidslinje)
            .setGjelderOpphør(true)
            .setNyeVurderinger(nyVilkårTidslinje)
            .setHistorikkAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .setSaksbehandlerIdent("Z000000");
        List<Historikkinnslag> historikkinnslag = tjeneste.lagHistorikkinnslag(fagsakId, input);
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.getFirst().getLinjer()).containsOnly(
            HistorikkinnslagLinje.tekst("Vilkåret ble vurdert uten endringer i utfall.", 0)
        );


    }
}
