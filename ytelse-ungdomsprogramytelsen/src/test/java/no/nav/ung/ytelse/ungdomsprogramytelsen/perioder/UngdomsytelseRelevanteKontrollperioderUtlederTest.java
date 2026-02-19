package no.nav.ung.ytelse.ungdomsprogramytelsen.perioder;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UngdomsytelseRelevanteKontrollperioderUtlederTest {

    public static final LocalDate PROGRAMPERIODE_FOM = LocalDate.of(2025, 12, 15);
    public static final LocalDate PROGRAMPERIODE_TOM = LocalDate.of(2026, 3, 15);
    @Inject
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;
    private Behandling behandling;


    @BeforeEach
    void setUp() {

        lagFagsakOgBehandling();

        ungdomsprogramPeriodeRepository.lagre(
            behandling.getId(),
            List.of(new UngdomsprogramPeriode(PROGRAMPERIODE_FOM, PROGRAMPERIODE_TOM))
        );

    }


    @Test
    void skal_finne_perioder_for_kontroll_der_deler_av_relevante_perioder_er_markert() {
        // Programperiode går fra desember til mars
        // Markerer kun januar for kontroll
        DatoIntervallEntitet januar = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, januar)));

        LocalDateTimeline<Boolean> perioderForKontroll = relevanteKontrollperioderUtleder.utledPerioderForKontrollAvInntekt(behandling.getId());

        assertEquals(new LocalDateTimeline<>(januar.toLocalDateInterval(), true), perioderForKontroll);
    }


    @Test
    void skal_finne_perioder_for_kontroll_der_alle_relevante_perioder_er_markert_med_kontroll_av_siste_periode() {
        // Programperiode går fra desember til mars
        // Markerer kun januar for kontroll
        DatoIntervallEntitet januar = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31));
        DatoIntervallEntitet februar = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28));
        DatoIntervallEntitet mars = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, januar),
            new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, februar),
            new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, mars)
        ));

        LocalDateTimeline<Boolean> perioderForKontroll = relevanteKontrollperioderUtleder.utledPerioderForKontrollAvInntekt(behandling.getId());

        LocalDateTimeline<Boolean> expected = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(januar.toLocalDateInterval(), true),
            new LocalDateSegment<>(februar.toLocalDateInterval(), true),
            new LocalDateSegment<>(mars.toLocalDateInterval(), true)
        ));

        assertEquals(expected, perioderForKontroll);
    }

    @Test
    void skal_finne_perioder_for_kontroll_der_alle_relevante_perioder_er_markert_med_kontroll_av_siste_periode_og_annen_trigger() {
        // Programperiode går fra desember til mars
        // Markerer kun januar for kontroll
        DatoIntervallEntitet januar = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31));
        DatoIntervallEntitet februar = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28));
        DatoIntervallEntitet mars = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, januar),
            new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, februar),
            new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, mars),
            new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 3, 1), PROGRAMPERIODE_TOM))
        ));

        LocalDateTimeline<Boolean> perioderForKontroll = relevanteKontrollperioderUtleder.utledPerioderForKontrollAvInntekt(behandling.getId());

        LocalDateTimeline<Boolean> expected = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(januar.toLocalDateInterval(), true),
            new LocalDateSegment<>(februar.toLocalDateInterval(), true),
            new LocalDateSegment<>(mars.toLocalDateInterval(), true)
        ));

        assertEquals(expected, perioderForKontroll);
    }


    @Test
    void skal_finne_første_periode_for_sammenhengende_ytelsestidslinje_med_tre_segmenter() {
        final var førstePeriodeFom = LocalDate.of(2023, 1, 12);
        final var førstePeriodeTom = LocalDate.of(2023, 1, 31);
        final var sistePeriodeFom = LocalDate.of(2023, 3, 1);
        final var sistePeriodeTom = LocalDate.of(2023, 3, 14);
        LocalDateTimeline<YearMonth> ytelsesPerioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(førstePeriodeFom, førstePeriodeTom, YearMonth.of(2023, 1)),
            new LocalDateSegment<>(førstePeriodeTom.plusDays(1), sistePeriodeFom.minusDays(1), YearMonth.of(2023, 2)),
            new LocalDateSegment<>(sistePeriodeFom, sistePeriodeTom, YearMonth.of(2023, 3))
        ));

        LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> result = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);

        assertEquals(1, result.toSegments().size());
        final var første = result.toSegments().first();
        assertTrue(første.getValue().gjelderFørstePeriode());
        assertThat(første.getFom()).isEqualTo(førstePeriodeFom);
        assertThat(første.getTom()).isEqualTo(førstePeriodeTom);
    }

    @Test
    void skal_finne_første_periode_for_sammenhengende_ytelsestidslinje_med_to_segmenter() {
        final var førstePeriodeFom = LocalDate.of(2023, 1, 13);
        final var førstePeriodeTom = LocalDate.of(2023, 1, 31);
        final var sistePeriodeFom = LocalDate.of(2023, 2, 1);
        final var sistePeriodeTom = LocalDate.of(2023, 2, 12);
        LocalDateTimeline<YearMonth> ytelsesPerioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(førstePeriodeFom, førstePeriodeTom, YearMonth.of(2023, 1)),
            new LocalDateSegment<>(sistePeriodeFom, sistePeriodeTom, YearMonth.of(2023, 2))
        ));

        LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> result = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);

        assertEquals(1, result.toSegments().size());
        final var første = result.toSegments().first();
        assertTrue(første.getValue().gjelderFørstePeriode());
        assertThat(første.getFom()).isEqualTo(førstePeriodeFom);
        assertThat(første.getTom()).isEqualTo(førstePeriodeTom);
    }

    @Test
    void skal_finne_første_perioder_der_ytelsestidslinjen_er_delt_i_to_med_med_to_segmenter_i_hver() {
        final var førstePeriode1Fom = LocalDate.of(2023, 1, 1);
        final var førstePeriode1Tom = LocalDate.of(2023, 1, 31);
        final var sistePeriode1Fom = LocalDate.of(2023, 2, 1);
        final var sistePeriode1Tom = LocalDate.of(2023, 2, 10);

        final var førstePeriode2Fom = LocalDate.of(2023, 3, 1);
        final var førstePeriode2Tom = LocalDate.of(2023, 3, 31);
        final var sistePeriode2Fom = LocalDate.of(2023, 4, 1);
        final var sistePeriode2Tom = LocalDate.of(2023, 4, 1);
        LocalDateTimeline<YearMonth> ytelsesPerioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(førstePeriode1Fom, førstePeriode1Tom, YearMonth.of(2023, 1)),
            new LocalDateSegment<>(sistePeriode1Fom, sistePeriode1Tom, YearMonth.of(2023, 2)),
            new LocalDateSegment<>(førstePeriode2Fom, førstePeriode2Tom, YearMonth.of(2023, 3)),
            new LocalDateSegment<>(sistePeriode2Fom, sistePeriode2Tom, YearMonth.of(2023, 4))
        ));

        LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> result = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);

        assertEquals(2, result.toSegments().size());
        final var iterator = result.toSegments().iterator();
        final var første1 = iterator.next();
        assertTrue(første1.getValue().gjelderFørstePeriode());
        assertThat(første1.getFom()).isEqualTo(førstePeriode1Fom);
        assertThat(første1.getTom()).isEqualTo(førstePeriode1Tom);

        final var første2 = iterator.next();
        assertTrue(første2.getValue().gjelderFørstePeriode());
        assertThat(første2.getFom()).isEqualTo(førstePeriode2Fom);
        assertThat(første2.getTom()).isEqualTo(førstePeriode2Tom);
    }

    @Test
    void skal_finne_første_perioder_for_der_ytelsestidslinjen_er_delt_i_to_med_med_tre_segmenter_i_hver() {
        final var førstePeriode1Fom = LocalDate.of(2023, 1, 1);
        final var førstePeriode1Tom = LocalDate.of(2023, 1, 31);
        final var sistePeriode1Fom = LocalDate.of(2023, 3, 1);
        final var sistePeriode1Tom = LocalDate.of(2023, 3, 10);

        final var førstePeriode2Fom = LocalDate.of(2023, 4, 1);
        final var førstePeriode2Tom = LocalDate.of(2023, 4, 30);
        final var sistePeriode2Fom = LocalDate.of(2023, 6, 1);
        final var sistePeriode2Tom = LocalDate.of(2023, 6, 2);
        LocalDateTimeline<YearMonth> ytelsesPerioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(førstePeriode1Fom, førstePeriode1Tom, YearMonth.of(2023, 1)),
            new LocalDateSegment<>(førstePeriode1Tom.plusDays(1), sistePeriode1Fom.minusDays(1), YearMonth.of(2023, 2)),
            new LocalDateSegment<>(sistePeriode1Fom, sistePeriode1Tom, YearMonth.of(2023, 3)),
            new LocalDateSegment<>(førstePeriode2Fom, førstePeriode2Tom, YearMonth.of(2023, 4)),
            new LocalDateSegment<>(førstePeriode2Tom.plusDays(1), sistePeriode2Fom.minusDays(1), YearMonth.of(2023, 5)),
            new LocalDateSegment<>(sistePeriode2Fom, sistePeriode2Tom, YearMonth.of(2023, 6))
        ));

        LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> result = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);

        assertEquals(2, result.toSegments().size());
        final var iterator = result.toSegments().iterator();
        final var første1 = iterator.next();
        assertTrue(første1.getValue().gjelderFørstePeriode());
        assertThat(første1.getFom()).isEqualTo(førstePeriode1Fom);


        final var første2 = iterator.next();
        assertTrue(første2.getValue().gjelderFørstePeriode());
        assertThat(første2.getFom()).isEqualTo(førstePeriode2Fom);
        assertThat(første2.getTom()).isEqualTo(førstePeriode2Tom);
    }

    private void lagFagsakOgBehandling() {
        final var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), PROGRAMPERIODE_FOM, PROGRAMPERIODE_TOM);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }
}
