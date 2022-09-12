package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.smurt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.PeriodeMedVarighet;

public class SykdomsperiodeEtablertTilsynSmørerTest {

    @Test
    public void tomPeriodeSkalFungere() {
        final LocalDateTimeline<Boolean> oppfyltSykdomTidslinje = new LocalDateTimeline<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1), Boolean.TRUE);
        
        final List<PeriodeMedVarighet> resultat = SykdomsperiodeEtablertTilsynSmører.smørEtablertTilsyn(oppfyltSykdomTidslinje, LocalDateTimeline.empty());
        assertThat(resultat).isEmpty();
    }
    
    @Test
    public void enDagSkalFungere() {
        final LocalDateTimeline<Boolean> oppfyltSykdomTidslinje = new LocalDateTimeline<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1), Boolean.TRUE);
        
        final List<PeriodeMedVarighet> resultat = SykdomsperiodeEtablertTilsynSmører.smørEtablertTilsyn(oppfyltSykdomTidslinje, toTimeline(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1), Duration.ofHours(1))
                ));
        
        assertThat(resultat).isNotEmpty();
        assertResultatEr(resultat,
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1)), Duration.ofHours(1))
        );
    }
    
    @Test
    public void enDagSomSkalSmøresOverHeleUken() {
        final LocalDateTimeline<Boolean> oppfyltSykdomTidslinje = new LocalDateTimeline<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 15), Boolean.TRUE);
        
        final List<PeriodeMedVarighet> resultat = SykdomsperiodeEtablertTilsynSmører.smørEtablertTilsyn(oppfyltSykdomTidslinje, toTimeline(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1), Duration.ofHours(1))
                ));
        
        assertThat(resultat).isNotEmpty();
        assertResultatEr(resultat,
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 5)), Duration.ofHours(1).dividedBy(5L))
        );
    }
    
    @Test
    public void toDagerSomSkalSmøresOverHeleUken() {
        final LocalDateTimeline<Boolean> oppfyltSykdomTidslinje = new LocalDateTimeline<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 15), Boolean.TRUE);
        
        final List<PeriodeMedVarighet> resultat = SykdomsperiodeEtablertTilsynSmører.smørEtablertTilsyn(oppfyltSykdomTidslinje, toTimeline(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1), Duration.ofHours(1)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 2), LocalDate.of(2022, 8, 2), Duration.ofHours(5))
                ));
        
        assertThat(resultat).isNotEmpty();
        assertResultatEr(resultat,
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 5)), Duration.ofHours(6).dividedBy(5L))
        );
    }
    
    @Test
    public void smøringGjøresPerUke() {
        final LocalDateTimeline<Boolean> oppfyltSykdomTidslinje = new LocalDateTimeline<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 15), Boolean.TRUE);
        
        final List<PeriodeMedVarighet> resultat = SykdomsperiodeEtablertTilsynSmører.smørEtablertTilsyn(oppfyltSykdomTidslinje, toTimeline(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 5), LocalDate.of(2022, 8, 15), Duration.ofHours(5))
                ));
        
        assertThat(resultat).isNotEmpty();
        assertResultatEr(resultat,
            // Dager uten oppgitt etablert tilsyn skal telles som 0 ved smøring:
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 5)), Duration.ofHours(5).dividedBy(5L)), 
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 8), LocalDate.of(2022, 8, 12)), Duration.ofHours(5)),
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 15), LocalDate.of(2022, 8, 15)), Duration.ofHours(5))
        );
    }
    
    @Test
    public void manglendeSykdomsvurderingSkalSplitteSmøringsperiode() {
        final LocalDateTimeline<Boolean> oppfyltSykdomTidslinje = new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 2), Boolean.TRUE),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 4), LocalDate.of(2022, 8, 5), Boolean.TRUE)
                ));      
        
        final List<PeriodeMedVarighet> resultat = SykdomsperiodeEtablertTilsynSmører.smørEtablertTilsyn(oppfyltSykdomTidslinje, toTimeline(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1), Duration.ofHours(1)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 2), LocalDate.of(2022, 8, 2), Duration.ofHours(5)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 3), LocalDate.of(2022, 8, 5), Duration.ofHours(5))
                ));
        
        assertThat(resultat).isNotEmpty();
        assertResultatEr(resultat,
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 2)), Duration.ofHours(6).dividedBy(2L)),
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 4), LocalDate.of(2022, 8, 5)), Duration.ofHours(5))
        );
    }
    
    @Test
    public void ikkeOppfyltSykdomSkalSplitteSmøringsperiode() {
        final LocalDateTimeline<Boolean> oppfyltSykdomTidslinje = new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 2), Boolean.TRUE),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 3), LocalDate.of(2022, 8, 3), Boolean.FALSE),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 4), LocalDate.of(2022, 8, 5), Boolean.TRUE)
                ));      
        
        final List<PeriodeMedVarighet> resultat = SykdomsperiodeEtablertTilsynSmører.smørEtablertTilsyn(oppfyltSykdomTidslinje, toTimeline(
                new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 1), Duration.ofHours(1)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 2), LocalDate.of(2022, 8, 2), Duration.ofHours(5)),
                new LocalDateSegment<>(LocalDate.of(2022, 8, 3), LocalDate.of(2022, 8, 5), Duration.ofHours(5))
                ));
        
        assertThat(resultat).isNotEmpty();
        assertResultatEr(resultat,
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 2)), Duration.ofHours(6).dividedBy(2L)),
            new PeriodeMedVarighet(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 8, 4), LocalDate.of(2022, 8, 5)), Duration.ofHours(5))
        );
    }

    private <V> void assertResultatEr(List<PeriodeMedVarighet> resultat, PeriodeMedVarighet... perioder) {
        assertThat(resultat.size()).isEqualTo(perioder.length);
        int index = 0;
        for (PeriodeMedVarighet actual: resultat) {
            assertThat(actual.getPeriode().getFomDato()).isEqualTo(perioder[index].getPeriode().getFomDato());
            assertThat(actual.getPeriode().getTomDato()).isEqualTo(perioder[index].getPeriode().getTomDato());
            assertThat(actual.getVarighet()).isEqualTo(perioder[index].getVarighet());
            index++;
        }
    }
    
    @SafeVarargs
    private static <T> LocalDateTimeline<T> toTimeline(LocalDateSegment<T>... segments) {
        return new LocalDateTimeline<T>(List.of(segments));
    }
}
