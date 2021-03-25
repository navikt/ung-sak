package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@CdiDbAwareTest
public class BehandlingDumpTest {

    @Inject
    private BehandlingDump behandlingDump;

    @Inject
    BehandlingRepositoryProvider behandlingRepositoryProvider;

    private TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMP);

    @Test
    void test_dump_behandling() throws Exception {
        Behandling behandling = scenario.lagre(behandlingRepositoryProvider);

        Fagsak fagsak = behandling.getFagsak();
        var dumpOutput = behandlingDump.dump(fagsak);

        assertThat(dumpOutput).isNotEmpty().hasSize(2);

        for (var c : dumpOutput) {
            System.out.println(c.getPath());
            System.out.println(c.getContent());
        }
    }

}
