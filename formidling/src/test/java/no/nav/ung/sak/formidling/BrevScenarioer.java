package no.nav.ung.sak.formidling;

import java.time.LocalDate;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestGrunnlag;

public class BrevScenarioer {

    static TestScenarioBuilder lagAvsluttetStandardBehandling(BehandlingRepositoryProvider repositoryProvider1, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository1, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository1) {
        UngTestGrunnlag ungTestGrunnlag = UngTestGrunnlag.standardInnvilget(LocalDate.of(2024, 12, 1));

        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestGrunnlag);

        var behandling = scenarioBuilder.buildOgLagreMedUng(repositoryProvider1, ungdomsytelseGrunnlagRepository1, ungdomsprogramPeriodeRepository1);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return scenarioBuilder;
    }

}
