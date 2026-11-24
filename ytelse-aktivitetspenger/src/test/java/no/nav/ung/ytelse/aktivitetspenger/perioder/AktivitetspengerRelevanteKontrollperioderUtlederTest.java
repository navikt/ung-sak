package no.nav.ung.ytelse.aktivitetspenger.perioder;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AktivitetspengerRelevanteKontrollperioderUtlederTest {

    private static final YearMonth JANUAR = YearMonth.of(2027, 1);

    @Inject
    private RelevanteKontrollperioderUtleder utleder;
    @Inject
    private EntityManager entityManager;

    @Test
    void skal_kontrollere_første_og_eneste_delvise_måned() {
        var perioder = new LocalDateTimeline<>(JANUAR.atDay(10), JANUAR.atDay(20), JANUAR);

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(perioder, FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(JANUAR.atDay(1), JANUAR.atEndOfMonth(),
            new RelevanteKontrollperioderUtleder.InfoOmRådata(false)));
    }

    @Test
    void skal_frita_første_og_eneste_delvise_måned_før_januar_2027() {
        var desember = YearMonth.of(2026, 12);
        var perioder = new LocalDateTimeline<>(desember.atDay(10), desember.atDay(20), desember);

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(perioder, FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_kontrollere_alle_måneder_også_etter_opphold() {
        var februar = JANUAR.plusMonths(1);
        var april = JANUAR.plusMonths(3);
        var perioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(JANUAR.atDay(1), JANUAR.atEndOfMonth(), JANUAR),
            new LocalDateSegment<>(februar.atDay(1), februar.atDay(10), februar),
            new LocalDateSegment<>(april.atDay(1), april.atEndOfMonth(), april)));

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(perioder, FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(JANUAR.atDay(1), JANUAR.atEndOfMonth(), new RelevanteKontrollperioderUtleder.InfoOmRådata(true)),
            new LocalDateSegment<>(februar.atDay(1), februar.atEndOfMonth(), new RelevanteKontrollperioderUtleder.InfoOmRådata(false)),
            new LocalDateSegment<>(april.atDay(1), april.atEndOfMonth(), new RelevanteKontrollperioderUtleder.InfoOmRådata(true)))));
    }

    @Test
    void skal_utlede_delvis_første_måned_fra_behandlingens_vilkår() {
        var repositories = AktivitetspengerTestRepositories.lagAlleAktivitetspengerTestRepositoriesOgAbakusTjeneste(entityManager, null);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunHøySats(JANUAR.atDay(10));
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medSøknadDato(JANUAR.atDay(10))
            .medAktivitetspengerTestGrunnlag(scenario)
            .buildOgLagreMedAktivitspenger(repositories);

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(behandling.getId());

        assertThat(resultat.segmenter()).first().satisfies(segment -> {
            assertThat(segment.getFom()).isEqualTo(JANUAR.atDay(1));
            assertThat(segment.getTom()).isEqualTo(JANUAR.atEndOfMonth());
            assertThat(segment.getValue().gjelderHelePerioden()).isFalse();
        });
    }
}
