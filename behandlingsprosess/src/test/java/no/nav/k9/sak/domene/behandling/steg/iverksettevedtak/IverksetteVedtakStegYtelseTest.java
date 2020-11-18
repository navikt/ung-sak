package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksettImpl;
import no.nav.k9.sak.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.k9.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IverksetteVedtakStegYtelseTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository ;

    private Behandling behandling;

    private Instance<OpprettProsessTaskIverksett> opprettProsessTaskIverksett;

    private Repository repository ;
    private BehandlingVedtakRepository behandlingVedtakRepository ;
    private HistorikkRepository historikkRepository ;

    @Mock
    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    @Mock
    private IdentifiserOverlappendeInfotrygdYtelseTjeneste iverksettingSkalIkkeStoppesAvOverlappendeYtelse;

    @Mock
    private FagsakProsessTaskRepository prosessTaskRepository;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;

    @Mock
    private InfotrygdFeedService infotrygdFeedService;

    private IverksetteVedtakSteg iverksetteVedtakSteg;

    @BeforeEach
    public void setup() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        repository =  new Repository(entityManager);
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        historikkRepository = repositoryProvider.getHistorikkRepository();


        opprettProsessTaskIverksett = new UnitTestLookupInstanceImpl<>(new OpprettProsessTaskIverksettImpl(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService));
        iverksetteVedtakSteg = new IverksetteVedtakSteg(repositoryProvider,
            opprettProsessTaskIverksett,
            iverksettingSkalIkkeStoppesAvOverlappendeYtelse,
            vurderBehandlingerUnderIverksettelse);
        behandling = opprettBehandling();
    }

    @Test
    public void vurder_gitt_venterPåInfotrygd_venterTidligereBehandling_skal_VENT_TIDLIGERE_BEHANDLING() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(true);
        when(iverksettingSkalIkkeStoppesAvOverlappendeYtelse.vurder(eq(behandling), any())).thenReturn(Optional.empty());

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.STARTET);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        Historikkinnslag historikkinnslag = historikkRepository.hentHistorikk(behandling.getId()).get(0);
        assertThat(historikkinnslag.getHistorikkinnslagDeler()).hasSize(1);
        HistorikkinnslagDel del1 = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del1.getHendelse())
            .hasValueSatisfying(hendelse -> assertThat(hendelse.getNavn()).as("navn").isEqualTo(HistorikkinnslagType.IVERKSETTELSE_VENT.getKode()));
        assertThat(del1.getAarsak().get()).isEqualTo(Venteårsak.VENT_TIDLIGERE_BEHANDLING.getKode());
    }

    @Test
    public void vurder_gitt_venterPåInfotrygd_ikkeVenterTidligereBehandling_skal_gi_empty_og_lagre_overlapp() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(false);
        BehandlingOverlappInfotrygd behandlingOverlappInfotrygd = mock(BehandlingOverlappInfotrygd.class);
        when(iverksettingSkalIkkeStoppesAvOverlappendeYtelse.vurder(eq(behandling), any())).thenReturn(Optional.of(behandlingOverlappInfotrygd));

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        verify(iverksettingSkalIkkeStoppesAvOverlappendeYtelse).vurderOgLagreEventueltOverlapp(any(), any());
    }

    @Test
    public void vurder_gitt_ikkeVenterPåInfotrygd_ikkeVenterTidligereBehandling_skal_gi_empty() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(false);
        when(iverksettingSkalIkkeStoppesAvOverlappendeYtelse.vurder(eq(behandling), any())).thenReturn(Optional.empty());

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        verify(iverksettingSkalIkkeStoppesAvOverlappendeYtelse).vurderOgLagreEventueltOverlapp(any(), any());
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        return iverksetteVedtakSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), lås));
    }

    private Behandling opprettBehandling() {
        var scenario = TestScenarioBuilder.builderMedSøknad().medBehandlingStegStart(BehandlingStegType.IVERKSETT_VEDTAK);

        Behandling behandling = scenario.lagre(repositoryProvider);

        repository.flush();

        return behandling;
    }

    private BehandlingVedtak opprettBehandlingVedtak(VedtakResultatType resultatType, IverksettingStatus iverksettingStatus) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
            .medAnsvarligSaksbehandler("E2354345")
            .medVedtakResultatType(resultatType)
            .medIverksettingStatus(iverksettingStatus)
            .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        return behandlingVedtak;
    }

}
