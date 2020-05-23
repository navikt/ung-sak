package no.nav.k9.sak.test.util.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.test.util.fagsak.FagsakBuilder;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class BehandlingRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final Repository repository = repoRule.getRepository();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BehandlingKandidaterRepository behandlingKandidaterRepository;

    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    private VedtakVarselRepository vedtakVarselRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();

    private Saksnummer saksnummer = new Saksnummer("2");
    private Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).medSaksnummer(saksnummer).build();
    private Behandling behandling;

    private LocalDateTime imorgen = LocalDateTime.now().plusDays(1);
    private LocalDateTime igår = LocalDateTime.now().minusDays(1);

    @Test
    public void skal_finne_behandling_gitt_id() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        Behandling resultat = behandlingRepository.hentBehandling(behandling.getId());

        // Assert
        assertThat(resultat).isNotNull();
    }

    private void lagreBehandling(Behandling... behandlinger) {
        for (Behandling behandling : behandlinger) {
            BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
            behandlingRepository.lagre(behandling, lås);
        }
    }

    @Test
    public void skal_hente_alle_behandlinger_fra_fagsak() {

        Behandling.Builder builder = opprettBuilderForBehandling();
        lagreBehandling(builder);

        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        assertThat(behandlinger).hasSize(1);

    }

    private void lagreBehandling(Behandling.Builder builder) {
        Behandling behandling = builder.build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    public void skal_finne_behandling_med_årsak() {
        Behandling behandling = opprettRevurderingsKandidat();

        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)).build();

        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        List<Behandling> result = behandlingRepository.hentBehandlingerMedÅrsakerForFagsakId(behandling.getFagsakId(),
            BehandlingÅrsakType.årsakerForAutomatiskRevurdering());
        assertThat(result).isNotEmpty();
    }

    @Test
    public void skal_hente_siste_behandling_basert_på_fagsakId() {

        Behandling.Builder builder = opprettBuilderForBehandling();

        lagreBehandling(builder);

        Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId());

        assertThat(sisteBehandling).isPresent();
        assertThat(sisteBehandling.get().getFagsakId()).isEqualTo(fagsak.getId());
    }

    @Test
    public void skal_hente_siste_innvilget_eller_endret_på_fagsakId() {
        BehandlingVedtak.Builder forVedtak = opprettBuilderForVedtak();
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingVedtakRepository.lagre(forVedtak.medIverksettingStatus(IverksettingStatus.IVERKSATT).build(), lås);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);

        Optional<Behandling> innvilgetBehandling = behandlingRepository.finnSisteInnvilgetBehandling(behandling.getFagsakId());

        assertThat(innvilgetBehandling).isPresent();
        assertThat(innvilgetBehandling.get().getFagsakId()).isEqualTo(behandling.getFagsak().getId());
    }

    @Test
    public void skal_kunne_lagre_vedtak() {
        BehandlingVedtak vedtak = opprettBuilderForVedtak().build();

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        behandlingRepository.lagre(behandling, lås);
        behandlingVedtakRepository.lagre(vedtak, lås);

        Long id = vedtak.getId();
        assertThat(id).isNotNull();

        repository.flushAndClear();
        BehandlingVedtak vedtakLest = repository.hent(BehandlingVedtak.class, id);
        assertThat(vedtakLest).isNotNull();

    }

    @Test
    public void skal_finne_behandling_gitt_korrekt_uuid() {
        // Arrange
        Behandling behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        Optional<Behandling> resultat = behandlingRepository.hentBehandlingHvisFinnes(behandling.getUuid());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(behandling);
    }

    @Test
    public void skal_ikke_finne_behandling_gitt_feil_uuid() {
        // Arrange
        Behandling behandling = opprettBuilderForBehandling().build();
        lagreBehandling(behandling);

        // Act
        Optional<Behandling> resultat = behandlingRepository.hentBehandlingHvisFinnes(UUID.randomUUID());

        // Assert
        assertThat(resultat).isNotPresent();
    }

    @Test
    public void skal_hente_liste_over_revurderingsaarsaker() {
        Map<String, VurderÅrsak> stringVurderÅrsakMap = VurderÅrsak.kodeMap();
        assertThat(stringVurderÅrsakMap).hasSize(5);
        assertThat(stringVurderÅrsakMap.containsValue(VurderÅrsak.FEIL_FAKTA)).isTrue();
    }

    private VedtakVarsel getBehandlingsresultat(Behandling behandling) {
        return vedtakVarselRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
    }

    @Test
    public void skal_finne_for_automatisk_gjenopptagelse_naar_alle_kriterier_oppfylt() {

        // Arrange
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, igår);

        Behandling behandling2 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling2, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);

        Behandling behandling3 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling3, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        lagreBehandling(behandling1, behandling2, behandling3);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).hasSize(3);
        assertThat(liste).contains(behandling1);
        assertThat(liste).contains(behandling2);
        assertThat(liste).contains(behandling3);
    }

    @Test
    public void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_naar_manuelt_aksjonspunkt() {

        // Arrange
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD, igår);
        lagreBehandling(behandling1);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_naar_lukket_aksjonspunkt() {

        LocalDateTime.now().minusDays(1);
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        Aksjonspunkt aksjonspunkt = opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "ferdig");
        lagreBehandling(behandling1);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_ikke_finne_for_automatisk_gjenopptagelse_naar_aksjonspunkt_frist_ikke_utgaatt() {

        // Arrange
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, imorgen);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_ikke_finne_for_automatisk_gjenopptagelse_når_aksjonspunt_er_avbrutt() throws Exception {
        // Arrange
        Behandling behandling = opprettBehandlingForAutomatiskGjenopptagelse();
        Aksjonspunkt aksjonspunkt = opprettAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        aksjonspunktTestSupport.setTilAvbrutt(aksjonspunkt);
        lagreBehandling(behandling);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_finne_for_gjenopplivelse_naar_alle_kriterier_oppfylt() {

        // Arrange
        Behandling behandling1 = opprettBehandlingForAutomatiskGjenopptagelse();
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        opprettAksjonspunkt(behandling1, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, igår);

        Behandling behandling2 = opprettBehandlingForAutomatiskGjenopptagelse();
        Aksjonspunkt ap2 = opprettAksjonspunkt(behandling2, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);

        Behandling behandling3 = opprettBehandlingForAutomatiskGjenopptagelse();
        Aksjonspunkt ap3 = opprettAksjonspunkt(behandling3, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, igår);
        lagreBehandling(behandling1, behandling2, behandling3);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();

        // Assert
        assertThat(liste).isEmpty();

        // Arrange
        aksjonspunktTestSupport.setTilUtført(ap2, "Begrunnelse");
        aksjonspunktTestSupport.setTilUtført(ap3, "Begrunnelse");
        lagreBehandling(behandling2, behandling3);

        // Act
        liste = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();

        // Assert
        assertThat(liste).hasSize(2);
        assertThat(liste).contains(behandling2);
        assertThat(liste).contains(behandling3);
    }

    @Test
    public void skal_finne_førstegangsbehandling_naar_frist_er_utgatt() {
        // Arrange
        LocalDate tidsfrist = LocalDate.now().minusDays(1);
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingstidFrist(tidsfrist);
        scenario.lagre(repositoryProvider);

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerMedUtløptBehandlingsfrist();

        // Assert
        assertThat(liste).hasSize(1);
    }

    @Test
    public void skal_ikke_finne_revurderingsbehandling() {
        // Arrange
        Behandling behandling = opprettRevurderingsKandidat();

        LocalDate tidsfrist = LocalDate.now().minusDays(1);
        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingstidFrist(tidsfrist).build();
        // Tidsfristen blir overstyrt
        revurderingsBehandling.setBehandlingstidFrist(tidsfrist);
        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerMedUtløptBehandlingsfrist();

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_finne_revurderingsbehandling_med_endringssøknad() {
        // Arrange
        Behandling behandling = opprettRevurderingsKandidat();

        LocalDate tidsfrist = LocalDate.now().minusDays(1);
        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingstidFrist(tidsfrist)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER))
            .build();
        // Tidsfristen blir overstyrt
        revurderingsBehandling.setBehandlingstidFrist(tidsfrist);
        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));
        // Act
        List<Behandling> liste = behandlingKandidaterRepository.finnBehandlingerMedUtløptBehandlingsfrist();
        // Assert
        assertThat(liste).hasSize(1);
    }

    @Test
    public void skal_opprettholde_id_etter_endringer() {

        // Lagre Personopplysning
        AbstractTestScenario<?> scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_finne_årsaker_for_behandling() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling()
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .medManueltOpprettet(false))
            .build();
        lagreBehandling(behandling);

        // Act
        List<BehandlingÅrsak> liste = behandlingRepository.finnÅrsakerForBehandling(behandling);

        // Assert
        assertThat(liste).hasSize(1);
        assertThat(liste.get(0).getBehandlingÅrsakType()).isEqualTo(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }

    @Test
    public void skal_finne_årsakstyper_for_behandling() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling()
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ANNET)
                .medManueltOpprettet(false))
            .build();
        lagreBehandling(behandling);

        // Act
        List<BehandlingÅrsakType> liste = behandlingRepository.finnÅrsakTyperForBehandling(behandling);

        // Assert
        assertThat(liste).hasSize(1);
        assertThat(liste.get(0)).isEqualTo(BehandlingÅrsakType.RE_ANNET);
    }

    @Test
    public void skal_ikke_finne_noen_årsakstyper_hvis_ingen() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling()
            .build();
        lagreBehandling(behandling);

        // Act
        List<BehandlingÅrsakType> liste = behandlingRepository.finnÅrsakTyperForBehandling(behandling);

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void skal_ikke_finne_noen_årsaker_hvis_ingen() {

        // Arrange
        Behandling behandling = opprettBuilderForBehandling()
            .build();
        lagreBehandling(behandling);

        // Act
        List<BehandlingÅrsak> liste = behandlingRepository.finnÅrsakerForBehandling(behandling);

        // Assert
        assertThat(liste).isEmpty();
    }

    @Test
    public void avsluttet_dato_skal_ha_dato_og_tid() {
        // Arrange
        LocalDateTime avsluttetDato = LocalDateTime.now();
        Behandling behandling = opprettBuilderForBehandling().medAvsluttetDato(avsluttetDato)
            .build();

        lagreBehandling(behandling);
        repository.flushAndClear();

        // Act
        Optional<Behandling> resultatBehandling = behandlingRepository.hentBehandlingHvisFinnes(behandling.getUuid());

        // Assert
        assertThat(resultatBehandling).isNotEmpty();
        LocalDateTime avsluttetDatoResultat = resultatBehandling.get().getAvsluttetDato();

        assertThat(avsluttetDatoResultat).isEqualTo(avsluttetDato.withNano(0)); // Oracle is not returning milliseconds.

    }

    private Behandling opprettBehandlingForAutomatiskGjenopptagelse() {

        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        return behandling;
    }

    private Aksjonspunkt opprettAksjonspunkt(Behandling behandling,
                                             AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             LocalDateTime frist) {

        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        aksjonspunktTestSupport.setFrist(aksjonspunkt, frist, Venteårsak.UDEFINERT, null);
        return aksjonspunkt;
    }

    private BehandlingVedtak.Builder opprettBuilderForVedtak() {
        behandling = opprettBehandlingMedTermindato();
        oppdaterMedBehandlingsresultatOgLagre(behandling, false);

        return BehandlingVedtak.builder(behandling.getId())
            .medAnsvarligSaksbehandler("Janne Hansen")
            .medIverksettingStatus(IverksettingStatus.IKKE_IVERKSATT);
    }

    private Behandling opprettBehandlingMedTermindato() {

        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);
        return behandling;
    }

    private Behandling opprettRevurderingsKandidat() {

        var scenario = TestScenarioBuilder.builderMedSøknad();

        behandling = scenario.lagre(repositoryProvider);
        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medAnsvarligSaksbehandler("asdf").build();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

    private VedtakVarsel oppdaterMedBehandlingsresultatOgLagre(Behandling behandling, boolean henlegg) {

        if (henlegg) {
            behandling.setBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET);
        }

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        return getBehandlingsresultat(behandling);
    }

    private Behandling.Builder opprettBuilderForBehandling() {
        fagsakRepository.opprettNy(fagsak);
        return Behandling.forFørstegangssøknad(fagsak);

    }
}
