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

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Inject
    private RelevanteKontrollperioderUtleder utleder;
    @Inject
    private EntityManager entityManager;

    @Test
    void skal_kontrollere_første_og_eneste_delvise_måned() {
        var perioder = new LocalDateTimeline<>(NOVEMBER.atDay(10), NOVEMBER.atDay(20), NOVEMBER);

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(perioder, FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(),
            new RelevanteKontrollperioderUtleder.InfoOmRådata(false)));
    }

    @Test
    void skal_kontrollere_alle_måneder_også_etter_opphold() {
        var desember = NOVEMBER.plusMonths(1);
        var februar = NOVEMBER.plusMonths(3);
        var perioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(), NOVEMBER),
            new LocalDateSegment<>(desember.atDay(1), desember.atDay(10), desember),
            new LocalDateSegment<>(februar.atDay(1), februar.atEndOfMonth(), februar)));

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(perioder, FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(NOVEMBER.atDay(1), NOVEMBER.atEndOfMonth(), new RelevanteKontrollperioderUtleder.InfoOmRådata(true)),
            new LocalDateSegment<>(desember.atDay(1), desember.atEndOfMonth(), new RelevanteKontrollperioderUtleder.InfoOmRådata(false)),
            new LocalDateSegment<>(februar.atDay(1), februar.atEndOfMonth(), new RelevanteKontrollperioderUtleder.InfoOmRådata(true)))));
    }

    @Test
    void skal_utlede_delvis_første_måned_fra_behandlingens_vilkår() {
        var repositories = AktivitetspengerTestRepositories.lagAlleAktivitetspengerTestRepositoriesOgAbakusTjeneste(entityManager, null);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunHøySats(NOVEMBER.atDay(10));
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medSøknadDato(NOVEMBER.atDay(10))
            .medAktivitetspengerTestGrunnlag(scenario)
            .buildOgLagreMedAktivitspenger(repositories);

        var resultat = utleder.utledPerioderRelevantForKontrollAvInntekt(behandling.getId());

        assertThat(resultat.segmenter()).first().satisfies(segment -> {
            assertThat(segment.getFom()).isEqualTo(NOVEMBER.atDay(1));
            assertThat(segment.getTom()).isEqualTo(NOVEMBER.atEndOfMonth());
            assertThat(segment.getValue().gjelderHelePerioden()).isFalse();
        });
    }
}
