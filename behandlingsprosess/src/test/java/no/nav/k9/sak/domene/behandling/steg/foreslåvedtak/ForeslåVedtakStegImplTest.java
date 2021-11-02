package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;


import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.dokument.bestill.tjenester.FormidlingDokumentdataTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class ForeslåVedtakStegImplTest {

    private ForeslåVedtakTjeneste foreslåVedtakTjeneste = mock(ForeslåVedtakTjeneste.class);
    private FormidlingDokumentdataTjeneste formidlingDokumentdataKlient = mock(FormidlingDokumentdataTjeneste.class);

    @Test
    public void skalKalleForeslåVedtakTjeneste() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagMocked();
        var behandlingRepository = scenario.mockBehandlingRepository();
        ForeslåVedtakStegImpl steg = new ForeslåVedtakStegImpl(behandlingRepository, foreslåVedtakTjeneste, formidlingDokumentdataKlient, new UnitTestLookupInstanceImpl<YtelsespesifikkForeslåVedtak>(b -> null));

        // Act
        Fagsak fagsak = behandling.getFagsak();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        steg.utførSteg(kontekst);

        // Assert
        verify(foreslåVedtakTjeneste).foreslåVedtak(eq(behandling), eq(kontekst));
    }

    @Test
    public void skalKalleTjenesteVedTilbakehoppTilFørForeslåVedtak() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagMocked();
        var behandlingRepository = scenario.mockBehandlingRepository();
        ForeslåVedtakStegImpl steg = new ForeslåVedtakStegImpl(behandlingRepository, foreslåVedtakTjeneste, formidlingDokumentdataKlient, new UnitTestLookupInstanceImpl<YtelsespesifikkForeslåVedtak>(b -> null));

        // Act
        Fagsak fagsak = behandling.getFagsak();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        steg.vedHoppOverBakover(kontekst, null, BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT, BehandlingStegType.FATTE_VEDTAK);

        // Assert
        verify(formidlingDokumentdataKlient).ryddVedTilbakehopp(eq(behandling.getId()));
    }
}
