package no.nav.ung.sak.domene.behandling.steg.simulering;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.InntrekkBeslutning;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelse;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseBehandlingInfoV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.db.util.Repository;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.ung.sak.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.ung.sak.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjenesteImpl;
import no.nav.ung.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;
import no.nav.ung.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class SimulerOppdragStegTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private BehandlingRepository behandlingRepository;
    private K9OppdragRestKlient k9OppdragRestKlientMock;
    private K9TilbakeRestKlient k9TilbakeRestKlientMock;
    private TilkjentYtelseTjeneste tilkjentYtelseTjenesteMock;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

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
        simuleringIntegrasjonTjeneste = new SimuleringIntegrasjonTjenesteImpl(tilkjentYtelseTjenesteMock, k9OppdragRestKlientMock);


        TilkjentYtelseBehandlingInfoV1 info = new TilkjentYtelseBehandlingInfoV1();
        TilkjentYtelse ty = new TilkjentYtelse(Collections.emptyList());
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

    @Test
    public void skal_reaktivere_inaktiv_svar_ved_aksjonspunkt() {
        when(k9TilbakeRestKlientMock.harÅpenTilbakekrevingsbehandling(any(Saksnummer.class))).thenReturn(false);
        var varseltekst = "Her er en fin varseltekst";
        tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, varseltekst));
        tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingValg(behandling);
        when(k9OppdragRestKlientMock.hentSimuleringResultat(any())).thenReturn(Optional.of(new SimuleringResultatDto(1000L, 0L, 0L,false)));

        steg = opprettSteg();

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);
        (new Repository(entityManager)).flushAndClear();

        assertThat(resultat.getAksjonspunktListe().size()).isEqualTo(1);
        assertThat(resultat.getAksjonspunktListe().getFirst()).isEqualTo(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);

        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isPresent();
        assertThat(tilbakekrevingValg.get().getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
        assertThat(tilbakekrevingValg.get().getVarseltekst()).isEqualTo(varseltekst);

    }


    private SimulerOppdragSteg opprettSteg() {
        return new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simuleringIntegrasjonTjeneste, tilbakekrevingRepository, k9TilbakeRestKlientMock);
    }
}
