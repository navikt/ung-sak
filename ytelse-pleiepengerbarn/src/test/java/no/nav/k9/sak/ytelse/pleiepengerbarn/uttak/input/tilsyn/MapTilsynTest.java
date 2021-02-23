package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.Tilsynsordning;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.TilsynsordningPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

class MapTilsynTest {

    private MapTilsyn mapper = new MapTilsyn();

    @Test
    void skal_mappe_tilsyn_innenfor_periode_til_vurdering() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(periodeTilVurdering, Duration.ZERO)),
            List.of(),
            List.of(new Tilsynsordning(List.of(new TilsynsordningPeriode(periodeTilVurdering, varighet)), Tilsynsordning.OppgittTilsynSvar.JA)),
            List.of()));

        var tilsynsMap = mapper.map(new TreeSet<>(kravDokumenter), perioderFraSøknader, tidlinjeTilVurdering);

        var lukketPeriode = new LukketPeriode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato());
        assertThat(tilsynsMap).containsOnlyKeys(lukketPeriode);
        assertThat(tilsynsMap).containsEntry(lukketPeriode, varighet);
    }

    @Test
    void skal_mappe_tilsyn_innenfor_periode_til_vurdering_2() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(periodeTilVurdering, Duration.ZERO)),
            List.of(),
            List.of(new Tilsynsordning(List.of(new TilsynsordningPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now()), varighet)), Tilsynsordning.OppgittTilsynSvar.JA)),
            List.of()));

        var tilsynsMap = mapper.map(new TreeSet<>(kravDokumenter), perioderFraSøknader, tidlinjeTilVurdering);

        var lukketPeriode = new LukketPeriode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato());
        assertThat(tilsynsMap).containsOnlyKeys(lukketPeriode);
        assertThat(tilsynsMap).containsEntry(lukketPeriode, varighet);
    }

    @Test
    void skal_mappe_tilsyn_innenfor_periode_til_vurdering_prioriter_siste_verdi() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var journalpostId1 = new JournalpostId(2L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD), new KravDokument(journalpostId1, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var periodeDel1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now().minusDays(30));
        var periodeDel2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(periodeDel1, Duration.ZERO)),
            List.of(),
            List.of(new Tilsynsordning(List.of(new TilsynsordningPeriode(periodeDel1, varighet)), Tilsynsordning.OppgittTilsynSvar.JA)),
            List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeDel2, Duration.ZERO)),
                List.of(),
                List.of(new Tilsynsordning(List.of(new TilsynsordningPeriode(periodeDel2, Duration.ZERO)), Tilsynsordning.OppgittTilsynSvar.JA)),
                List.of()));

        var tilsynsMap = mapper.map(new TreeSet<>(kravDokumenter), perioderFraSøknader, tidlinjeTilVurdering);

        var lukketPeriode = new LukketPeriode(periodeDel1.getFomDato(), periodeDel1.getTomDato().minusDays(1));
        var lukketPeriode1 = new LukketPeriode(periodeDel2.getFomDato(), periodeDel2.getTomDato());
        assertThat(tilsynsMap).containsOnlyKeys(lukketPeriode, lukketPeriode1);
        assertThat(tilsynsMap).containsEntry(lukketPeriode, varighet);
        assertThat(tilsynsMap).containsEntry(lukketPeriode1, Duration.ZERO);
    }
}
