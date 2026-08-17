package no.nav.ung.ytelse.aktivitetspenger.del1.avkort;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AvkortTjenesteTest {

    @Test
    void skal_gi_tom_tidslinje_når_grunnlag_ikke_finnes() {
        LocalDateTimeline<Boolean> tidslinje = AvkortTjeneste.utledTidslinjeForMuligAvkorting(null);

        assertThat(tidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_gi_tom_tidslinje_når_ingen_startdatoer_finnes() {
        StartdatoGrunnlag grunnlag = grunnlagMedStartdatoer();

        LocalDateTimeline<Boolean> tidslinje = AvkortTjeneste.utledTidslinjeForMuligAvkorting(grunnlag);

        assertThat(tidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_gi_tidslinje_på_52_uker_fra_dagen_etter_startdato() {
        LocalDate startdato = LocalDate.of(2026, 1, 1);
        StartdatoGrunnlag grunnlag = grunnlagMedStartdatoer(startdato);

        LocalDateTimeline<Boolean> tidslinje = AvkortTjeneste.utledTidslinjeForMuligAvkorting(grunnlag);

        assertThat(tidslinje.isEmpty()).isFalse();
        assertThat(tidslinje.getMinLocalDate()).isEqualTo(startdato.plusDays(1));
        assertThat(tidslinje.getMaxLocalDate()).isEqualTo(startdato.plusWeeks(52).minusDays(1));
        assertThat(tidslinje.stream()).allMatch(segment -> Boolean.TRUE.equals(segment.getValue()));
    }

    @Test
    void skal_bruke_siste_soekte_startdato_når_flere_finnes() {
        LocalDate tidligste = LocalDate.of(2026, 1, 1);
        LocalDate seneste = LocalDate.of(2026, 3, 1);
        StartdatoGrunnlag grunnlag = grunnlagMedStartdatoer(tidligste, seneste);

        LocalDateTimeline<Boolean> tidslinje = AvkortTjeneste.utledTidslinjeForMuligAvkorting(grunnlag);

        assertThat(tidslinje.getMinLocalDate()).isEqualTo(seneste.plusDays(1));
        assertThat(tidslinje.getMaxLocalDate()).isEqualTo(seneste.plusWeeks(52).minusDays(1));
    }

    private static StartdatoGrunnlag grunnlagMedStartdatoer(LocalDate... startdatoer) {
        List<SøktStartdato> søkteStartdatoer = new java.util.ArrayList<>();
        long journalpostId = 1L;
        for (LocalDate startdato : startdatoer) {
            søkteStartdatoer.add(new SøktStartdato(startdato, new JournalpostId(journalpostId++)));
        }
        StartdatoGrunnlag grunnlag = Mockito.mock(StartdatoGrunnlag.class);
        when(grunnlag.getRelevanteStartdatoer()).thenReturn(new Startdatoer(søkteStartdatoer));
        return grunnlag;
    }
}

