package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.smurt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynPeriode;

public class EtablertTilsynUnntaksutnullerTest {

    @Test
    public void nattevåkOgBeredskapSkalFjerneOmsorgstilbudstimene() {
        final LocalDateTimeline<Duration> etablertTilsynTidslinje = toTimeline(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1), Duration.ofHours(1)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 2), LocalDate.of(2022, 8, 2), Duration.ofHours(5)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 3), LocalDate.of(2022, 8, 5), Duration.ofHours(5))
                );

        final UnntakEtablertTilsynForPleietrengende unntakEtablertTilsynForPleietrengende = new UnntakEtablertTilsynForPleietrengende(
                new AktørId("dummy"),
                new UnntakEtablertTilsyn(List.of(
                    new UnntakEtablertTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 4), LocalDate.of(2022, 8, 4)), "", Resultat.OPPFYLT, new AktørId("dummy"), 1L, "nav", LocalDateTime.now()),
                    new UnntakEtablertTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 5), LocalDate.of(2022, 8, 5)), "", Resultat.IKKE_OPPFYLT, new AktørId("dummy"), 1L, "nav", LocalDateTime.now())
                ), List.of()),
                new UnntakEtablertTilsyn(List.of(
                    new UnntakEtablertTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1)), "", Resultat.OPPFYLT, new AktørId("dummy"), 1L, "nav", LocalDateTime.now())
                ), List.of()));

        final LocalDateTimeline<Duration> resultat = EtablertTilsynUnntaksutnuller.ignorerEtablertTilsynVedInnleggelserOgUnntak(etablertTilsynTidslinje,
                Optional.of(unntakEtablertTilsynForPleietrengende), List.of());

        assertThat(resultat).isEqualTo(toTimeline(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1), Duration.ofHours(0)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 2), LocalDate.of(2022, 8, 3), Duration.ofHours(5)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 4), LocalDate.of(2022, 8, 4), Duration.ofHours(0)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 5), LocalDate.of(2022, 8, 5), Duration.ofHours(5))
                ));
    }

    @SafeVarargs
    private static <T> LocalDateTimeline<T> toTimeline(LocalDateSegment<T>... segments) {
        return new LocalDateTimeline<T>(List.of(segments));
    }
}
