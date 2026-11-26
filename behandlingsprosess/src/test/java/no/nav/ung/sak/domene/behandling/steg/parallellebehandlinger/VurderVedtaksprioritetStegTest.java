package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.BEREGN_YTELSE;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FATTE_VEDTAK;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FORESLÅ_VEDTAK;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.INNHENT_REGISTEROPP;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.IVERKSETT_VEDTAK;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.SIMULER_OPPDRAG;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.START_STEG;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_KOMPLETTHET;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_VEDTAKSPRIORITET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VurderVedtaksprioritetStegTest {

    /**
     * Rekkefølgen i modellene med ventepunktet for vedtaksprioritet. Steg som mangler, gir exception, som i
     * {@code BehandlingModellImpl.indexOf}.
     */
    static final List<BehandlingStegType> MODELL_MED_VENTEPUNKT_FOR_VEDTAKSPRIORITET = List.of(START_STEG, INNHENT_REGISTEROPP, VURDER_KOMPLETTHET, KONTROLLER_REGISTER_INNTEKT,
        FORESLÅ_BEHANDLINGSRESULTAT, VURDER_VEDTAKSPRIORITET, BEREGN_YTELSE, SIMULER_OPPDRAG, FORESLÅ_VEDTAK, FATTE_VEDTAK, IVERKSETT_VEDTAK);

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private SynkroniserÅpneBehandlingerTaskOppretter synkroniseringTaskOppretter;
    private VurderVedtaksprioritetSteg steg;

    private Behandling førstegangsbehandling;
    private LocalDateTime nesteVedtakstidspunkt = LocalDateTime.now().minusDays(1);

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager, behandlingRepository);
        synkroniseringTaskOppretter = mock(SynkroniserÅpneBehandlingerTaskOppretter.class);

        var tjeneste = new VedtaksprioritetTjeneste(new ParallelleBehandlingerRepository(entityManager), behandlingskontrollMedModell(MODELL_MED_VENTEPUNKT_FOR_VEDTAKSPRIORITET));
        steg = new VurderVedtaksprioritetSteg(behandlingRepository, new FagsakLåsRepository(entityManager), tjeneste, synkroniseringTaskOppretter);

        førstegangsbehandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
    }

    @Test
    void passerer_når_fagsaken_ikke_har_vedtak_og_ingen_andre_behandlinger() {
        settSteg(førstegangsbehandling, VURDER_VEDTAKSPRIORITET);

        var resultat = steg.utførSteg(kontekst(førstegangsbehandling));

        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        verify(synkroniseringTaskOppretter, never()).opprettHvisIkkeFinnes(anyLong());
    }

    @Test
    void passerer_når_behandlingen_bygger_på_siste_vedtak() {
        avslutt(førstegangsbehandling);
        var revurdering = nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, VURDER_VEDTAKSPRIORITET);

        var resultat = steg.utførSteg(kontekst(revurdering));

        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
    }

    @Test
    void venter_og_ber_om_synkronisering_når_behandlingen_ikke_bygger_på_siste_vedtak() {
        avslutt(førstegangsbehandling);
        var kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);
        var revurdering = nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, FORESLÅ_VEDTAK);
        avslutt(revurdering);

        var resultat = steg.utførSteg(kontekst(kontroll));

        assertVenter(resultat);
        verify(synkroniseringTaskOppretter).opprettHvisIkkeFinnes(kontroll.getFagsakId());
    }

    @Test
    void venter_på_annen_behandling_som_står_i_beregning() {
        avslutt(førstegangsbehandling);
        nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, BEREGN_YTELSE);
        var kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);

        var resultat = steg.utførSteg(kontekst(kontroll));

        assertVenter(resultat);
        verify(synkroniseringTaskOppretter, never()).opprettHvisIkkeFinnes(anyLong());
    }

    @Test
    void ser_annen_behandling_med_vedtaksprioritet_selv_om_sesjonen_har_gammel_tilstand() {
        avslutt(førstegangsbehandling);
        var revurdering = nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, VURDER_KOMPLETTHET);
        var kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);

        // Gitt at revurderingen har gått til beregning i en annen transaksjon etter at sesjonen lastet den
        entityManager.createNativeQuery("update behandling_steg_tilstand set behandling_steg = :steg where behandling_id = :id and aktiv = true")
            .setParameter("steg", BEREGN_YTELSE.getKode())
            .setParameter("id", revurdering.getId())
            .executeUpdate();
        assertThat(behandlingRepository.hentBehandling(revurdering.getId()).getAktivtBehandlingSteg()).isEqualTo(VURDER_KOMPLETTHET);

        var resultat = steg.utførSteg(kontekst(kontroll));

        assertVenter(resultat);
    }

    @Test
    void venter_på_annen_behandling_som_fatter_vedtak() {
        avslutt(førstegangsbehandling);
        var revurdering = nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, FATTE_VEDTAK);
        settStatus(revurdering, BehandlingStatus.FATTER_VEDTAK);
        var kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);

        var resultat = steg.utførSteg(kontekst(kontroll));

        assertVenter(resultat);
    }

    @Test
    void venter_på_originalbehandling_under_iverksetting() {
        var kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);
        lagreVedtak(førstegangsbehandling);
        settStatus(førstegangsbehandling, BehandlingStatus.IVERKSETTER_VEDTAK);

        var resultat = steg.utførSteg(kontekst(kontroll));

        assertVenter(resultat);
        verify(synkroniseringTaskOppretter, never()).opprettHvisIkkeFinnes(anyLong());
    }

    @Test
    void passerer_når_annen_behandling_venter_i_ventepunktet_for_vedtaksprioritet() {
        avslutt(førstegangsbehandling);
        nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, VURDER_VEDTAKSPRIORITET);
        var kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);

        var resultat = steg.utførSteg(kontekst(kontroll));

        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
    }

    @Test
    void passerer_når_annen_behandling_står_før_ventepunktet_for_vedtaksprioritet() {
        avslutt(førstegangsbehandling);
        nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, VURDER_KOMPLETTHET);
        var kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);

        var resultat = steg.utførSteg(kontekst(kontroll));

        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
    }

    @Test
    void henlagt_behandling_har_ikke_vedtaksprioritet() {
        avslutt(førstegangsbehandling);
        var revurdering = nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, SIMULER_OPPDRAG);
        settResultat(revurdering, BehandlingResultatType.HENLAGT_MASKINELT);
        var kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);

        var resultat = steg.utførSteg(kontekst(kontroll));

        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
    }

    @Test
    void ny_kjøring_etter_vent_venter_igjen_til_behandlingen_med_vedtaksprioritet_er_avsluttet() {
        avslutt(førstegangsbehandling);
        var kontroll = nyBehandling(førstegangsbehandling, BehandlingType.KONTROLL, VURDER_VEDTAKSPRIORITET);
        var revurdering = nyBehandling(førstegangsbehandling, BehandlingType.REVURDERING, SIMULER_OPPDRAG);

        assertVenter(steg.utførSteg(kontekst(kontroll)));
        assertVenter(steg.utførSteg(kontekst(kontroll)));

        avslutt(revurdering);
        oppdaterOriginalBehandling(kontroll, revurdering);

        assertThat(steg.utførSteg(kontekst(kontroll)).getAksjonspunktResultater()).isEmpty();
    }

    static BehandlingskontrollTjeneste behandlingskontrollMedModell(List<BehandlingStegType> modell) {
        var behandlingskontrollTjeneste = mock(BehandlingskontrollTjeneste.class);
        when(behandlingskontrollTjeneste.sammenlignRekkefølge(any(), any(), any(), any())).thenAnswer(invocation -> {
            int a = indexOf(modell, invocation.getArgument(2));
            int b = indexOf(modell, invocation.getArgument(3));
            return Integer.compare(a, b);
        });
        return behandlingskontrollTjeneste;
    }

    private static int indexOf(List<BehandlingStegType> modell, BehandlingStegType steg) {
        int index = modell.indexOf(steg);
        if (index < 0) {
            throw new IllegalArgumentException("Steg " + steg + " finnes ikke i modellen");
        }
        return index;
    }

    private static void assertVenter(BehandleStegResultat resultat) {
        assertThat(resultat.getAksjonspunktResultater()).singleElement().satisfies(ap -> {
            assertThat(ap.getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_VEDTAKSPRIORITET);
            assertThat(ap.getVenteårsak()).isEqualTo(Venteårsak.VENT_TIDLIGERE_BEHANDLING);
            assertThat(ap.getFrist()).isAfter(LocalDateTime.now());
        });
    }

    private BehandlingskontrollKontekst kontekst(Behandling behandling) {
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling));
    }

    private Behandling nyBehandling(Behandling original, BehandlingType type, BehandlingStegType aktivtSteg) {
        var behandling = Behandling.fraTidligereBehandling(original, type).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        settSteg(behandling, aktivtSteg);
        return behandling;
    }

    private void settSteg(Behandling behandling, BehandlingStegType steg) {
        new InternalManipulerBehandling().forceOppdaterBehandlingSteg(behandling, steg);
        lagreOgFlush(behandling);
    }

    private void settStatus(Behandling behandling, BehandlingStatus status) {
        Whitebox.setInternalState(behandling, "status", status);
        lagreOgFlush(behandling);
    }

    private void settResultat(Behandling behandling, BehandlingResultatType resultatType) {
        behandling.setBehandlingResultatType(resultatType);
        lagreOgFlush(behandling);
    }

    private void oppdaterOriginalBehandling(Behandling behandling, Behandling nyOriginal) {
        behandling.oppdaterOriginalBehandling(nyOriginal);
        lagreOgFlush(behandling);
    }

    private void avslutt(Behandling behandling) {
        lagreVedtak(behandling);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        lagreOgFlush(behandling);
    }

    private void lagreVedtak(Behandling behandling) {
        nesteVedtakstidspunkt = nesteVedtakstidspunkt.plusMinutes(1);
        var vedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakstidspunkt(nesteVedtakstidspunkt)
            .medAnsvarligSaksbehandler("Z000000")
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));
        entityManager.flush();
    }

    private void lagreOgFlush(Behandling behandling) {
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        entityManager.flush();
    }
}
