package no.nav.k9.sak.domene.behandling.steg.simulering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.InntrekkBeslutning;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelse;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseBehandlingInfoV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.k9.sak.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.k9.sak.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;
import no.nav.k9.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;

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

        TilkjentYtelseBehandlingInfoV1 info = new TilkjentYtelseBehandlingInfoV1();
        TilkjentYtelse ty = new TilkjentYtelse(LocalDate.now(), Collections.emptyList());
        InntrekkBeslutning ib = new InntrekkBeslutning(true);
        TilkjentYtelseOppdrag tyo = new TilkjentYtelseOppdrag(ty, info, ib);
        when(tilkjentYtelseTjenesteMock.hentTilkjentYtelseOppdrag(behandling)).thenReturn(tyo);
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
