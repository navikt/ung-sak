package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.ytelse.aktivitetspenger.testdata.VilkårUtfall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AktivitetspengerFinnSakerForInntektkontrollTest {

    private static final LocalDate NOVEMBER_START = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOVEMBER_SLUTT = LocalDate.of(2026, 11, 30);
    private static final LocalDate DESEMBER_SLUTT = LocalDate.of(2026, 12, 31);

    @Inject
    private EntityManager entityManager;
    @Inject
    private FinnSakerForInntektkontroll finnSakerForInntektkontroll;

    @Test
    void skal_velge_delvis_innvilget_november_og_ikke_helt_avslått_eller_innvilget_utenfor_måneden() {
        var repositories = AktivitetspengerTestRepositories.lagAlleAktivitetspengerTestRepositoriesOgAbakusTjeneste(entityManager, null);
        var delvisInnvilget = lagreScenario(repositories, scenario(NOVEMBER_START.plusDays(8), NOVEMBER_START.plusDays(9)));
        lagreScenario(repositories, scenario(DESEMBER_SLUTT, LocalDate.of(2027, 1, 1)));
        lagreScenario(repositories, scenario(NOVEMBER_SLUTT, NOVEMBER_START.plusMonths(1)));

        List<Fagsak> fagsaker = finnSakerForInntektkontroll.finnFagsaker(NOVEMBER_START, NOVEMBER_SLUTT);

        assertThat(fagsaker).containsExactly(delvisInnvilget.getFagsak());
    }

    private static Behandling lagreScenario(
        AktivitetspengerTestRepositories repositories,
        AktivitetspengerTestScenario scenario) {
        return AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medSøknadDato(NOVEMBER_START)
            .medAktivitetspengerTestGrunnlag(scenario)
            .buildOgLagreMedAktivitspenger(repositories);
    }

    private static AktivitetspengerTestScenario scenario(LocalDate avslåttTom, LocalDate innvilgetFom) {
        var fagsakPeriode = new LocalDateTimeline<>(NOVEMBER_START, DESEMBER_SLUTT, VilkårUtfall.oppfylt());
        var bostedsvilkår = new ArrayList<LocalDateSegment<VilkårUtfall>>();
        if (!avslåttTom.isBefore(NOVEMBER_START)) {
            bostedsvilkår.add(new LocalDateSegment<>(
                NOVEMBER_START, avslåttTom, VilkårUtfall.avslått(Avslagsårsak.AVKORTET)));
        }
        if (!innvilgetFom.isAfter(DESEMBER_SLUTT)) {
            bostedsvilkår.add(new LocalDateSegment<>(
                innvilgetFom, DESEMBER_SLUTT, VilkårUtfall.oppfylt()));
        }

        return AktivitetspengerTestScenario.builder()
            .medNavn("Test")
            .medSøknadsperioder(List.of(new Periode(NOVEMBER_START, DESEMBER_SLUTT)))
            .medFødselsdato(NOVEMBER_START.minusYears(30))
            .medVilkår(VilkårType.ALDERSVILKÅR, fagsakPeriode)
            .medVilkår(VilkårType.BOSTEDSVILKÅR, new LocalDateTimeline<>(bostedsvilkår))
            .medVilkår(VilkårType.BISTANDSVILKÅR, fagsakPeriode)
            .medVilkår(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, fagsakPeriode)
            .build();
    }
}
