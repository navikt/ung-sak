package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Bygger en avsluttet førstegangsbehandling og en revurdering på samme fagsak, slik at brevregler som
 * sammenligner med forrige vedtak har noe å sammenligne mot.
 */
public class AktivitetspengerRevurderingTestOppsett {

    private AktivitetspengerRevurderingTestOppsett() {
    }

    public static Behandling lagRevurdering(AktivitetspengerTestRepositories repositories,
                                            AktivitetspengerTestScenario originalScenario,
                                            AktivitetspengerTestScenario revurderingScenario) {
        var original = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(originalScenario)
            .buildOgLagreMedAktivitspenger(repositories);
        original.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        original.avsluttBehandling();

        var revurdering = AktivitetspengerTestScenarioBuilder.builderMedSøknad(original.getFagsak().getAktørId())
            .medAktivitetspengerTestGrunnlag(revurderingScenario)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(original, førsteÅrsak(revurderingScenario))
            .buildOgLagreMedAktivitspenger(repositories);
        revurdering.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        revurdering.avsluttBehandling();
        return revurdering;
    }

    public static AktivitetspengerTestScenario medEndretDagsats(AktivitetspengerTestScenario scenario, BigDecimal tillegg) {
        var endret = scenario.tilkjentYtelsePerioder().mapValue(it -> new TilkjentYtelseVerdi(
            it.uredusertBeløp(), it.reduksjon(), it.redusertBeløp(),
            it.dagsats().add(tillegg), it.utbetalingsgrad(), it.tilkjentBeløp()));

        return new AktivitetspengerTestScenario(scenario.navn(), scenario.søknadsperioder(), scenario.satsperioder(),
            scenario.beregningsgrunnlag(), endret, scenario.aldersvilkår(), scenario.fødselsdato(),
            scenario.behandlingTriggere(), scenario.barn(), scenario.dødsdato(), scenario.kontrollerInntektPerioder(),
            scenario.vilkår(), scenario.inngangsvilkårVurderinger(), scenario.bostedsAvklaringer(), scenario.vilkårsavklaringer());
    }

    public static AktivitetspengerTestScenario utenVilkårsavklaringer(AktivitetspengerTestScenario scenario) {
        return new AktivitetspengerTestScenario(scenario.navn(), scenario.søknadsperioder(), scenario.satsperioder(),
            scenario.beregningsgrunnlag(), scenario.tilkjentYtelsePerioder(), scenario.aldersvilkår(), scenario.fødselsdato(),
            scenario.behandlingTriggere(), scenario.barn(), scenario.dødsdato(), scenario.kontrollerInntektPerioder(),
            scenario.vilkår(), scenario.inngangsvilkårVurderinger(), List.of(), Map.of());
    }

    private static BehandlingÅrsakType førsteÅrsak(AktivitetspengerTestScenario scenario) {
        return scenario.behandlingTriggere().stream().map(Trigger::getÅrsak).findFirst().orElseThrow();
    }
}
