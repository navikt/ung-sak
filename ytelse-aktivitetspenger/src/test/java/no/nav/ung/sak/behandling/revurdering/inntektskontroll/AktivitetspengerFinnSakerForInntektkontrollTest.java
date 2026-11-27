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

    private static final LocalDate JANUAR_START = LocalDate.of(2027, 1, 1);
    private static final LocalDate JANUAR_SLUTT = LocalDate.of(2027, 1, 31);
    private static final LocalDate FEBRUAR_SLUTT = LocalDate.of(2027, 2, 28);

    @Inject
    private EntityManager entityManager;
    @Inject
    private FinnSakerForInntektkontroll finnSakerForInntektkontroll;

    @Test
    void skal_velge_delvis_innvilget_januar_og_ikke_helt_avslått_eller_innvilget_utenfor_måneden() {
        var repositories = AktivitetspengerTestRepositories.lagAlleAktivitetspengerTestRepositoriesOgAbakusTjeneste(entityManager, null);
        var delvisInnvilget = lagreScenario(repositories, scenario(JANUAR_START.plusDays(8), JANUAR_START.plusDays(9)));
        lagreScenario(repositories, scenario(FEBRUAR_SLUTT, LocalDate.of(2027, 3, 1)));
        lagreScenario(repositories, scenario(JANUAR_SLUTT, JANUAR_START.plusMonths(1)));

        List<Fagsak> fagsaker = finnSakerForInntektkontroll.finnFagsaker(JANUAR_START, JANUAR_SLUTT);

        assertThat(fagsaker).containsExactly(delvisInnvilget.getFagsak());
    }

    private static Behandling lagreScenario(
        AktivitetspengerTestRepositories repositories,
        AktivitetspengerTestScenario scenario) {
        return AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medSøknadDato(JANUAR_START)
            .medAktivitetspengerTestGrunnlag(scenario)
            .buildOgLagreMedAktivitspenger(repositories);
    }

    private static AktivitetspengerTestScenario scenario(LocalDate avslåttTom, LocalDate innvilgetFom) {
        var fagsakPeriode = new LocalDateTimeline<>(JANUAR_START, FEBRUAR_SLUTT, VilkårUtfall.oppfylt());
        var bostedsvilkår = new ArrayList<LocalDateSegment<VilkårUtfall>>();
        if (!avslåttTom.isBefore(JANUAR_START)) {
            bostedsvilkår.add(new LocalDateSegment<>(
                JANUAR_START, avslåttTom, VilkårUtfall.avslått(Avslagsårsak.AVKORTET)));
        }
        if (!innvilgetFom.isAfter(FEBRUAR_SLUTT)) {
            bostedsvilkår.add(new LocalDateSegment<>(
                innvilgetFom, FEBRUAR_SLUTT, VilkårUtfall.oppfylt()));
        }

        return AktivitetspengerTestScenario.builder()
            .medNavn("Test")
            .medSøknadsperioder(List.of(new Periode(JANUAR_START, FEBRUAR_SLUTT)))
            .medFødselsdato(JANUAR_START.minusYears(30))
            .medVilkår(VilkårType.ALDERSVILKÅR, fagsakPeriode)
            .medVilkår(VilkårType.BOSTEDSVILKÅR, new LocalDateTimeline<>(bostedsvilkår))
            .medVilkår(VilkårType.BISTANDSVILKÅR, fagsakPeriode)
            .medVilkår(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, fagsakPeriode)
            .build();
    }
}
