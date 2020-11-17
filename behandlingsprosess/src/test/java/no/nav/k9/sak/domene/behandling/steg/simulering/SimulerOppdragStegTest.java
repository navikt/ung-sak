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

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.k9.sak.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.k9.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;
import no.nav.k9.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class SimulerOppdragStegTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider ;
    private TilbakekrevingRepository tilbakekrevingRepository ;
    private BehandlingRepository behandlingRepository ;
    private K9OppdragRestKlient k9OppdragRestKlientMock ;
    private K9TilbakeRestKlient k9TilbakeRestKlientMock ;
    private TilkjentYtelseTjeneste tilkjentYtelseTjenesteMock ;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste ;

    private SimulerOppdragSteg steg;
    private Behandling behandling;
    private BehandlingskontrollKontekst kontekst;

    @BeforeEach
    public void setup() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        tilbakekrevingRepository = new TilbakekrevingRepository(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        k9OppdragRestKlientMock = mock(K9OppdragRestKlient.class);
        k9TilbakeRestKlientMock = mock(K9TilbakeRestKlient.class);
        tilkjentYtelseTjenesteMock = mock(TilkjentYtelseTjeneste.class);
        behandlingProsesseringTjeneste = mock(BehandlingProsesseringTjeneste.class);

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
        tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, "varsel"));
        (new Repository(entityManager)).flushAndClear();

        steg = opprettSteg();

        // Act
        steg.vedHoppOverBakover(kontekst, mock(BehandlingStegModell.class), BehandlingStegType.VURDER_UTTAK, BehandlingStegType.FATTE_VEDTAK);
        (new Repository(entityManager)).flushAndClear();

        // Assert
        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isNotPresent();
    }

    @Test
    public void skal_kalle_kanseller_oppdrag_ved_tilbakehopp() {
        // Arrange
        steg = new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simuleringIntegrasjonTjeneste, tilbakekrevingRepository, k9TilbakeRestKlientMock);

        // Act
        steg.vedHoppOverBakover(kontekst, null, null, null);

        // Verify
        verify(k9OppdragRestKlientMock).kansellerSimulering(behandling.getUuid());
    }

    @Test
    public void skal__ikke_kalle_kanseller_oppdrag_ved_tilbakehopp_tilSimulerOppdragSteget() {
        // Arrange
        steg = new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simuleringIntegrasjonTjeneste, tilbakekrevingRepository, k9TilbakeRestKlientMock);

        // Act
        steg.vedHoppOverBakover(kontekst, null, BehandlingStegType.SIMULER_OPPDRAG, null);

        // Verify
        verify(k9OppdragRestKlientMock, never()).kansellerSimulering(behandling.getUuid());
    }

    @Test
    public void utførSteg_lagrer_tilbakekrevingoppdater_hvis_det_er_en_åpen_tilbakekreving() {
        when(k9TilbakeRestKlientMock.harÅpenTilbakekrevingsbehandling(any(Saksnummer.class))).thenReturn(true);

        steg = opprettSteg();

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);
        (new Repository(entityManager)).flushAndClear();

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isPresent();
        assertThat(tilbakekrevingValg.get().getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.TILBAKEKR_OPPDATER);
    }

    private SimulerOppdragSteg opprettSteg() {
        return new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simuleringIntegrasjonTjeneste, tilbakekrevingRepository, k9TilbakeRestKlientMock);
    }
}
