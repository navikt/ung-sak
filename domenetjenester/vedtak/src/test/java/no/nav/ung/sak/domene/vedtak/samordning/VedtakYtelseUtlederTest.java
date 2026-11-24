package no.nav.ung.sak.domene.vedtak.samordning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelse;
import no.nav.ung.sak.behandlingslager.samordning.YtelseAnvist;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.vedtak.samordning.BarnetilleggTidslinjeTjeneste.Barnetillegg;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.ytelseperioder.KvalifiserteYtelsesperioderTjeneste;

class VedtakYtelseUtlederTest {

    private static final Long BEHANDLING_ID = 1L;
    private static final UUID BEHANDLING_UUID = UUID.randomUUID();
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final Saksnummer SAKSNUMMER = new Saksnummer("ABC123");
    private static final LocalDateTime VEDTAKSTIDSPUNKT = LocalDateTime.of(2026, 3, 10, 12, 0);

    private static final LocalDate JAN_FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_TOM = LocalDate.of(2026, 1, 31);
    private static final LocalDate FEB_FOM = LocalDate.of(2026, 2, 1);
    private static final LocalDate FEB_TOM = LocalDate.of(2026, 2, 28);
    private static final LocalDate MAR_FOM = LocalDate.of(2026, 3, 1);
    private static final int VIRKEDAGER_JAN = 22;
    private static final int VIRKEDAGER_FEB = 20;
    private static final LocalDate APR_TOM = LocalDate.of(2026, 4, 30);

    private final BehandlingVedtakRepository behandlingVedtakRepository = mock();
    private final VilkårTjeneste vilkårTjeneste = mock();
    private final TilkjentYtelseRepository tilkjentYtelseRepository = mock();
    private final BarnetilleggTidslinjeTjeneste barnetilleggTjeneste = mock();
    private final KvalifiserteYtelsesperioderTjeneste kvalifiserteYtelsesperioderTjeneste = mock();
    private final Behandling behandling = mock();

    private VedtakYtelseUtleder utleder;

    @BeforeEach
    void setUp() {
        utleder = new VedtakYtelseUtleder(behandlingVedtakRepository, vilkårTjeneste, tilkjentYtelseRepository,
            new UnitTestLookupInstanceImpl<>(barnetilleggTjeneste), new UnitTestLookupInstanceImpl<>(kvalifiserteYtelsesperioderTjeneste));

        Fagsak fagsak = mock();
        when(fagsak.getSaksnummer()).thenReturn(SAKSNUMMER);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(behandling.getUuid()).thenReturn(BEHANDLING_UUID);
        when(behandling.getAktørId()).thenReturn(AKTØR_ID);
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.AKTIVITETSPENGER);

        BehandlingVedtak vedtak = mock();
        when(vedtak.getVedtakstidspunkt()).thenReturn(VEDTAKSTIDSPUNKT);
        when(vedtak.getVedtaksdato()).thenReturn(VEDTAKSTIDSPUNKT.toLocalDate());
        when(behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(BEHANDLING_ID)).thenReturn(Optional.of(vedtak));

        when(tilkjentYtelseRepository.hentTidslinje(BEHANDLING_ID)).thenReturn(LocalDateTimeline.empty());
        when(barnetilleggTjeneste.hentBarnetilleggTidslinje(BEHANDLING_ID)).thenReturn(LocalDateTimeline.empty());
        when(kvalifiserteYtelsesperioderTjeneste.finnPeriodeTidslinje(BEHANDLING_ID))
            .thenReturn(new LocalDateTimeline<>(LocalDateInterval.TIDENES_BEGYNNELSE, LocalDateInterval.TIDENES_ENDE, true));
    }

    @Test
    void skal_sette_vedtakshode_fra_behandling_og_vedtak() {
        medVilkår(segment(JAN_FOM, JAN_TOM, Utfall.OPPFYLT));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getAktørId()).isEqualTo(AKTØR_ID);
        assertThat(resultat.getSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(resultat.getYtelseType()).isEqualTo(FagsakYtelseType.AKTIVITETSPENGER);
        assertThat(resultat.getVedtakReferanse()).isEqualTo(BEHANDLING_UUID);
        assertThat(resultat.getVedtattTidspunkt()).isEqualTo(VEDTAKSTIDSPUNKT);
    }

    @Test
    void skal_bruke_hele_innvilget_periode_som_vedtaksperiode_også_fremtidige_måneder() {
        medVilkår(segment(JAN_FOM, APR_TOM, Utfall.OPPFYLT));
        medTilkjentYtelse(tilkjent(JAN_FOM, JAN_TOM, 800, 100, 16_000));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, APR_TOM));
        assertThat(resultat.getAnvisninger()).extracting(YtelseAnvist::getPeriode).containsExactly(
            DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM),
            DatoIntervallEntitet.fraOgMedTilOgMed(FEB_FOM, APR_TOM));
    }

    @Test
    void skal_gi_tom_dagsats_for_innvilgede_måneder_som_ikke_er_beregnet() {
        medVilkår(segment(JAN_FOM, APR_TOM, Utfall.OPPFYLT));
        medTilkjentYtelse(tilkjent(JAN_FOM, JAN_TOM, 800, 100, 16_000));

        var resultat = utleder.utled(behandling);

        var ikkeBeregnet = anvisningFra(resultat, FEB_FOM);
        assertThat(ikkeBeregnet.erBeregnet()).isFalse();
        assertThat(ikkeBeregnet.getDagsats()).isEmpty();
        assertThat(ikkeBeregnet.getBeløp()).isEmpty();
    }

    @Test
    void skal_ta_med_beregnet_måned_med_0_kr_utbetalt() {
        medVilkår(segment(JAN_FOM, JAN_TOM, Utfall.OPPFYLT));
        medTilkjentYtelse(tilkjent(JAN_FOM, JAN_TOM, 0, 0, 0));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.harInnvilgetPeriode()).isTrue();
        var nullUtbetaling = anvisningFra(resultat, JAN_FOM);
        assertThat(nullUtbetaling.erBeregnet()).isTrue();
        assertThat(nullUtbetaling.getDagsats()).hasValueSatisfying(d -> assertThat(d).isZero());
        assertThat(nullUtbetaling.getBeløp()).hasValueSatisfying(b -> assertThat(b).isZero());
    }

    @Test
    void skal_bruke_dagsats_fra_tilkjent_ytelse_og_beløp_som_dagsats_ganger_virkedager() {
        medVilkår(segment(JAN_FOM, JAN_TOM, Utfall.OPPFYLT));
        medTilkjentYtelse(tilkjent(JAN_FOM, JAN_TOM, 800, 60, 800 * VIRKEDAGER_JAN));

        var resultat = utleder.utled(behandling);

        var anvisning = anvisningFra(resultat, JAN_FOM);
        assertThat(anvisning.getDagsats()).hasValueSatisfying(d -> assertThat(d).isEqualByComparingTo("800"));
        assertThat(anvisning.getBeløp()).hasValueSatisfying(b -> assertThat(b).isEqualByComparingTo(BigDecimal.valueOf(800 * VIRKEDAGER_JAN)));
    }

    @Test
    void skal_sette_barnetillegg_også_for_innvilgede_måneder_som_ikke_er_beregnet() {
        medVilkår(segment(JAN_FOM, FEB_TOM, Utfall.OPPFYLT));
        medTilkjentYtelse(tilkjent(JAN_FOM, JAN_TOM, 800, 100, 800 * VIRKEDAGER_JAN));
        when(barnetilleggTjeneste.hentBarnetilleggTidslinje(BEHANDLING_ID))
            .thenReturn(new LocalDateTimeline<>(JAN_FOM, FEB_TOM, new Barnetillegg(2, 74)));

        var resultat = utleder.utled(behandling);

        var ikkeBeregnet = anvisningFra(resultat, FEB_FOM);
        assertThat(ikkeBeregnet.erBeregnet()).isFalse();
        assertThat(ikkeBeregnet.getAntallBarn()).contains(2);
        assertThat(ikkeBeregnet.getDagsatsBarnetillegg()).contains(74);
    }

    @Test
    void skal_splitte_anvisning_når_barnetillegg_endres_midt_i_måned() {
        var fødselsdato = LocalDate.of(2026, 1, 15);
        medVilkår(segment(JAN_FOM, JAN_TOM, Utfall.OPPFYLT));
        medTilkjentYtelse(tilkjent(JAN_FOM, JAN_TOM, 800, 100, 800 * VIRKEDAGER_JAN));
        when(barnetilleggTjeneste.hentBarnetilleggTidslinje(BEHANDLING_ID)).thenReturn(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(JAN_FOM, fødselsdato.minusDays(1), new Barnetillegg(0, 0)),
            new LocalDateSegment<>(fødselsdato, JAN_TOM, new Barnetillegg(1, 37)))));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getAnvisninger()).hasSize(2);
        var førFødsel = anvisningFra(resultat, JAN_FOM);
        assertThat(førFødsel.getPeriode().getTomDato()).isEqualTo(fødselsdato.minusDays(1));
        assertThat(førFødsel.getAntallBarn()).contains(0);
        assertThat(førFødsel.getDagsatsBarnetillegg()).contains(0);
        assertThat(førFødsel.getBeløp()).hasValueSatisfying(b -> assertThat(b).isEqualByComparingTo(BigDecimal.valueOf(800 * 10)));
        var etterFødsel = anvisningFra(resultat, fødselsdato);
        assertThat(etterFødsel.getPeriode().getTomDato()).isEqualTo(JAN_TOM);
        assertThat(etterFødsel.getAntallBarn()).contains(1);
        assertThat(etterFødsel.getDagsatsBarnetillegg()).contains(37);
        assertThat(etterFødsel.getBeløp()).hasValueSatisfying(b -> assertThat(b).isEqualByComparingTo(BigDecimal.valueOf(800 * 12)));
    }

    @Test
    void skal_slå_sammen_like_nabomåneder() {
        medVilkår(segment(JAN_FOM, FEB_TOM, Utfall.OPPFYLT));
        medTilkjentYtelse(
            tilkjent(JAN_FOM, JAN_TOM, 800, 100, 800 * VIRKEDAGER_JAN),
            tilkjent(FEB_FOM, FEB_TOM, 800, 100, 800 * VIRKEDAGER_FEB));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getAnvisninger()).extracting(YtelseAnvist::getPeriode)
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, FEB_TOM));
        assertThat(resultat.getAnvisninger().getFirst().getBeløp())
            .hasValueSatisfying(b -> assertThat(b).isEqualByComparingTo(BigDecimal.valueOf(800 * (VIRKEDAGER_JAN + VIRKEDAGER_FEB))));
    }

    @Test
    void skal_splitte_anvisning_når_dagsats_endres() {
        medVilkår(segment(JAN_FOM, FEB_TOM, Utfall.OPPFYLT));
        medTilkjentYtelse(
            tilkjent(JAN_FOM, JAN_TOM, 800, 100, 800 * VIRKEDAGER_JAN),
            tilkjent(FEB_FOM, FEB_TOM, 600, 75, 600 * VIRKEDAGER_FEB));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getAnvisninger()).extracting(YtelseAnvist::getPeriode).containsExactly(
            DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM),
            DatoIntervallEntitet.fraOgMedTilOgMed(FEB_FOM, FEB_TOM));
    }

    @Test
    void skal_korte_ned_vedtaksperioden_ved_opphør() {
        var opphørsdato = LocalDate.of(2026, 2, 15);
        medVilkår(
            segment(JAN_FOM, opphørsdato.minusDays(1), Utfall.OPPFYLT),
            segment(opphørsdato, APR_TOM, Utfall.IKKE_OPPFYLT));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, opphørsdato.minusDays(1)));
        assertThat(resultat.getAnvisninger()).allSatisfy(a -> assertThat(a.getPeriode().getTomDato()).isBefore(opphørsdato));
    }

    @Test
    void skal_forlenge_vedtaksperioden_ved_forlenget_periode() {
        var nyMaksdato = LocalDate.of(2026, 6, 30);
        medVilkår(segment(JAN_FOM, nyMaksdato, Utfall.OPPFYLT));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getPeriode().getTomDato()).isEqualTo(nyMaksdato);
    }

    @Test
    void skal_utelate_avslått_delperiode() {
        medVilkår(
            segment(JAN_FOM, JAN_TOM, Utfall.OPPFYLT),
            segment(FEB_FOM, FEB_TOM, Utfall.IKKE_OPPFYLT),
            segment(MAR_FOM, APR_TOM, Utfall.OPPFYLT));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, APR_TOM));
        assertThat(resultat.getAnvisninger()).extracting(YtelseAnvist::getPeriode).containsExactly(
            DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM),
            DatoIntervallEntitet.fraOgMedTilOgMed(MAR_FOM, APR_TOM));
    }

    @Test
    void skal_ikke_ta_med_tilkjent_ytelse_utenfor_innvilget_periode() {
        medVilkår(
            segment(JAN_FOM, JAN_TOM, Utfall.OPPFYLT),
            segment(FEB_FOM, FEB_TOM, Utfall.IKKE_OPPFYLT));
        medTilkjentYtelse(
            tilkjent(JAN_FOM, JAN_TOM, 800, 100, 16_000),
            tilkjent(FEB_FOM, FEB_TOM, 800, 100, 16_000));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getAnvisninger()).extracting(YtelseAnvist::getPeriode)
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM));
    }

    @Test
    void skal_utelate_perioder_som_er_avslått_i_uttak() {
        var førsteAvslåtteDag = LocalDate.of(2026, 2, 15);
        medVilkår(segment(JAN_FOM, APR_TOM, Utfall.OPPFYLT));
        medTilkjentYtelse(tilkjent(JAN_FOM, JAN_TOM, 800, 100, 800 * VIRKEDAGER_JAN));
        when(kvalifiserteYtelsesperioderTjeneste.finnPeriodeTidslinje(BEHANDLING_ID))
            .thenReturn(new LocalDateTimeline<>(JAN_FOM, førsteAvslåtteDag.minusDays(1), true));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, førsteAvslåtteDag.minusDays(1)));
        assertThat(resultat.getAnvisninger()).extracting(YtelseAnvist::getPeriode).containsExactly(
            DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM),
            DatoIntervallEntitet.fraOgMedTilOgMed(FEB_FOM, førsteAvslåtteDag.minusDays(1)));
    }

    @Test
    void skal_gi_vedtaksdato_som_periode_og_ingen_anvisninger_når_hele_innvilget_periode_er_avslått_i_uttak() {
        medVilkår(segment(JAN_FOM, APR_TOM, Utfall.OPPFYLT));
        when(kvalifiserteYtelsesperioderTjeneste.finnPeriodeTidslinje(BEHANDLING_ID)).thenReturn(LocalDateTimeline.empty());

        var resultat = utleder.utled(behandling);

        assertThat(resultat.harInnvilgetPeriode()).isFalse();
        assertThat(resultat.getAnvisninger()).isEmpty();
        assertThat(resultat.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(VEDTAKSTIDSPUNKT.toLocalDate(), VEDTAKSTIDSPUNKT.toLocalDate()));
    }

    @Test
    void skal_gi_vedtaksdato_som_periode_og_ingen_anvisninger_ved_fullt_avslag() {
        medVilkår(segment(JAN_FOM, APR_TOM, Utfall.IKKE_OPPFYLT));

        var resultat = utleder.utled(behandling);

        assertThat(resultat.harInnvilgetPeriode()).isFalse();
        assertThat(resultat.getAnvisninger()).isEmpty();
        assertThat(resultat.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(VEDTAKSTIDSPUNKT.toLocalDate(), VEDTAKSTIDSPUNKT.toLocalDate()));
    }

    @Test
    void skal_gi_vedtaksdato_som_periode_og_ingen_anvisninger_uten_vilkårsresultat() {
        when(vilkårTjeneste.samletVilkårsresultat(BEHANDLING_ID)).thenReturn(LocalDateTimeline.empty());

        var resultat = utleder.utled(behandling);

        assertThat(resultat.getAnvisninger()).isEmpty();
        assertThat(resultat.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(VEDTAKSTIDSPUNKT.toLocalDate(), VEDTAKSTIDSPUNKT.toLocalDate()));
    }

    @SafeVarargs
    private void medVilkår(LocalDateSegment<VilkårUtfallSamlet>... segmenter) {
        when(vilkårTjeneste.samletVilkårsresultat(BEHANDLING_ID)).thenReturn(new LocalDateTimeline<>(List.of(segmenter)));
    }

    @SafeVarargs
    private void medTilkjentYtelse(LocalDateSegment<TilkjentYtelseVerdi>... segmenter) {
        when(tilkjentYtelseRepository.hentTidslinje(BEHANDLING_ID)).thenReturn(new LocalDateTimeline<>(List.of(segmenter)));
    }

    private static LocalDateSegment<VilkårUtfallSamlet> segment(LocalDate fom, LocalDate tom, Utfall utfall) {
        return new LocalDateSegment<>(fom, tom, new VilkårUtfallSamlet(utfall, List.of()));
    }

    private static LocalDateSegment<TilkjentYtelseVerdi> tilkjent(LocalDate fom, LocalDate tom, int dagsats, int utbetalingsgrad, int tilkjentBeløp) {
        var beløp = BigDecimal.valueOf(tilkjentBeløp);
        return new LocalDateSegment<>(fom, tom, new TilkjentYtelseVerdi(beløp, BigDecimal.ZERO, beløp,
            BigDecimal.valueOf(dagsats), BigDecimal.valueOf(utbetalingsgrad), beløp));
    }

    private static YtelseAnvist anvisningFra(VedtakYtelse vedtak, LocalDate fom) {
        return vedtak.getAnvisninger().stream()
            .filter(a -> a.getPeriode().getFomDato().equals(fom))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Fant ingen anvisning som starter " + fom + " i " + vedtak.getAnvisninger()));
    }
}
