package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;

/**
 * Bygger en avsluttet førstegangsbehandling og en revurdering på samme fagsak, slik at brevregler som
 * sammenligner med forrige vedtak har noe å sammenligne mot.
 */
public class AktivitetspengerRevurderingTestOppsett {

    private AktivitetspengerRevurderingTestOppsett() {
    }

    public static Behandling lagRevurdering(AktivitetspengerTestRepositories repositories,
                                            AktivitetspengerTestScenario originalScenario,
                                            AktivitetspengerTestScenario revurderingScenario,
                                            BehandlingDel... behandlingsansvarlige) {
        var original = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(originalScenario)
            .buildOgLagreMedAktivitspenger(repositories);
        original.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        original.avsluttBehandling();

        var revurdering = AktivitetspengerTestScenarioBuilder.builderMedSøknad(original.getFagsak().getAktørId())
            .medAktivitetspengerTestGrunnlag(revurderingScenario)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(original, førsteÅrsak(revurderingScenario))
            .medBehandlingsansvarlig(behandlingsansvarlige)
            .buildOgLagreMedAktivitspenger(repositories);
        revurdering.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        revurdering.avsluttBehandling();
        return revurdering;
    }

    private static BehandlingÅrsakType førsteÅrsak(AktivitetspengerTestScenario scenario) {
        return scenario.behandlingTriggere().stream().map(Trigger::getÅrsak).findFirst().orElseThrow();
    }
}
