package no.nav.ung.sak.behandlingslager.behandling.startdato;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.ung.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.ung.sak.typer.JournalpostId;

/**
 * Regresjonstest for at {@link StartdatoGrunnlag} sine innholdsfelter er merket med
 * {@code @ChangeTracked}. Uten denne merkingen traverserer ikke diff-mekanismen
 * (brukt av registerinnhenting/reposisjonering, jf. {@link RegisterdataDiffsjekker})
 * inn i feltene, og en ny søknad blir da aldri oppdaget som en endring - selv om
 * {@link Startdatoer#getStartdatoer()} lenger ned faktisk er merket med
 * {@code @ChangeTracked}.
 */
class StartdatoGrunnlagDiffTest {

    private final RegisterdataDiffsjekker diffsjekker = new RegisterdataDiffsjekker(true);

    @Test
    void skal_oppdage_diff_når_oppgitte_startdatoer_har_fått_ny_søknad() {
        var grunnlag1 = new StartdatoGrunnlag(1L);
        grunnlag1.leggTil(List.of(new SøktStartdato(LocalDate.of(2026, 1, 1), new JournalpostId(1L))));

        var grunnlag2 = new StartdatoGrunnlag(1L);
        grunnlag2.leggTil(List.of(
            new SøktStartdato(LocalDate.of(2026, 1, 1), new JournalpostId(1L)),
            new SøktStartdato(LocalDate.of(2026, 6, 1), new JournalpostId(2L))));

        assertThat(diffsjekker.erForskjellPå(grunnlag1, grunnlag2)).isTrue();
    }

    @Test
    void skal_oppdage_diff_når_relevante_startdatoer_endres() {
        var grunnlag1 = new StartdatoGrunnlag(1L);
        grunnlag1.setRelevanteStartdatoer(new Startdatoer(List.of(new SøktStartdato(LocalDate.of(2026, 1, 1), new JournalpostId(1L)))));

        var grunnlag2 = new StartdatoGrunnlag(1L);
        grunnlag2.setRelevanteStartdatoer(new Startdatoer(List.of(new SøktStartdato(LocalDate.of(2026, 6, 1), new JournalpostId(2L)))));

        assertThat(diffsjekker.erForskjellPå(grunnlag1, grunnlag2)).isTrue();
    }

    @Test
    void skal_ikke_oppdage_diff_for_identisk_innhold() {
        var grunnlag1 = new StartdatoGrunnlag(1L);
        grunnlag1.leggTil(List.of(new SøktStartdato(LocalDate.of(2026, 1, 1), new JournalpostId(1L))));

        var grunnlag2 = new StartdatoGrunnlag(1L);
        grunnlag2.leggTil(List.of(new SøktStartdato(LocalDate.of(2026, 1, 1), new JournalpostId(1L))));

        assertThat(diffsjekker.erForskjellPå(grunnlag1, grunnlag2)).isFalse();
    }
}
