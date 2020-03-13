package no.nav.k9.sak.domene.behandling.steg.simulering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.foreldrepenger.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.foreldrepenger.økonomi.tilbakekreving.modell.TilbakekrevingValg;
import no.nav.foreldrepenger.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.domene.behandling.steg.simulering.SimulerOppdragSteg;
import no.nav.k9.sak.typer.Saksnummer;

public class SimulerOppdragStegTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private TilbakekrevingRepository tilbakekrevingRepository = new TilbakekrevingRepository(entityManager);
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    private SimulerOppdragSteg steg;
    private K9OppdragRestKlient k9OppdragRestKlientMock = mock(K9OppdragRestKlient.class);
    private FptilbakeRestKlient fptilbakeRestKlientMock = mock(FptilbakeRestKlient.class);
    private TilkjentYtelseTjeneste tilkjentYtelseTjenesteMock = mock(TilkjentYtelseTjeneste.class);
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste = mock(BehandlingProsesseringTjeneste.class);

    private Behandling behandling;
    private BehandlingskontrollKontekst kontekst;

    @Before
    public void setup() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);
        kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling));
        simuleringIntegrasjonTjeneste = new SimuleringIntegrasjonTjeneste(tilkjentYtelseTjenesteMock, k9OppdragRestKlientMock);
    }

    @Test
    public void deaktiverer_eksisterende_tilbakekrevingValg_ved_hopp_over_bakover() {
        // Arrange
        tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, "varsel"));
        repoRule.getRepository().flushAndClear();

        steg = opprettSteg();

        // Act
        steg.vedHoppOverBakover(kontekst, mock(BehandlingStegModell.class), BehandlingStegType.VURDER_UTTAK, BehandlingStegType.FATTE_VEDTAK);
        repoRule.getRepository().flushAndClear();

        // Assert
        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isNotPresent();
    }

    @Test
    public void skal_kalle_kanseller_oppdrag_ved_tilbakehopp() {
        // Arrange
        steg = new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simuleringIntegrasjonTjeneste, tilbakekrevingRepository, fptilbakeRestKlientMock);

        // Act
        steg.vedHoppOverBakover(kontekst, null, null, null);

        // Verify
        verify(k9OppdragRestKlientMock).kansellerSimulering(behandling.getUuid());
    }

    @Test
    public void skal__ikke_kalle_kanseller_oppdrag_ved_tilbakehopp_tilSimulerOppdragSteget() {
        // Arrange
        steg = new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simuleringIntegrasjonTjeneste, tilbakekrevingRepository, fptilbakeRestKlientMock);

        // Act
        steg.vedHoppOverBakover(kontekst, null, BehandlingStegType.SIMULER_OPPDRAG, null);

        // Verify
        verify(k9OppdragRestKlientMock, never()).kansellerSimulering(behandling.getUuid());
    }

    @Test
    public void utførSteg_lagrer_tilbakekrevingoppdater_hvis_det_er_en_åpen_tilbakekreving() {
        when(fptilbakeRestKlientMock.harÅpenTilbakekrevingsbehandling(any(Saksnummer.class))).thenReturn(true);

        steg = opprettSteg();

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);
        repoRule.getRepository().flushAndClear();

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isPresent();
        assertThat(tilbakekrevingValg.get().getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.TILBAKEKR_OPPDATER);
    }

    private SimulerOppdragSteg opprettSteg() {
        return new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simuleringIntegrasjonTjeneste, tilbakekrevingRepository, fptilbakeRestKlientMock);
    }
}
