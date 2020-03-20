package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;


import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakStegImpl;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class ForeslåVedtakStegImplTest {

    private ForeslåVedtakTjeneste foreslåVedtakTjeneste = mock(ForeslåVedtakTjeneste.class);

    @Test
    public void skalKalleTjeneste() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagMocked();
        var behandlingRepository = scenario.mockBehandlingRepository();
        ForeslåVedtakStegImpl steg = new ForeslåVedtakStegImpl(behandlingRepository, foreslåVedtakTjeneste);

        // Act
        Fagsak fagsak = behandling.getFagsak();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        steg.utførSteg(kontekst);

        // Assert
        verify(foreslåVedtakTjeneste).foreslåVedtak(eq(behandling), eq(kontekst));
    }
}
