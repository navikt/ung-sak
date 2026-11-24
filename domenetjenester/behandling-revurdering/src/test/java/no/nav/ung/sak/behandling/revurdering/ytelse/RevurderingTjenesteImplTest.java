package no.nav.ung.sak.behandling.revurdering.ytelse;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.*;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.ung.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.ung.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.ung.sak.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.ung.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class RevurderingTjenesteImplTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private HistorikkinnslagRepository historikkRepository;

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    @Inject
    private FagsakLåsRepository fagsakLåsRepository;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    @Any
    private Instance<GrunnlagKopierer> grunnlagKopierer;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        opprettRevurderingsKandidat();
    }

    @Test
    public void skal_opprette_revurdering() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        scenario.medBehandlingstidFrist(LocalDate.now().minusDays(5));
        Behandling behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);

        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);

        var revurderingTjeneste = lagRevurderingTjeneste(false);

        // Act
        Behandling revurdering = revurderingTjeneste
            .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes, BehandlingÅrsakType.RE_ANNET);

        // Assert
        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType()).isEqualTo(BehandlingÅrsakType.RE_ANNET);
        assertThat(revurdering.getType()).isEqualTo(BehandlingType.REVURDERING);
        assertThat(revurdering.getAksjonspunkter()).isEmpty();
        assertThat(revurdering.getBehandlingstidFrist()).isNotEqualTo(behandlingSomSkalRevurderes.getBehandlingstidFrist());
        assertThat(historikkRepository.hent(revurdering.getId())).extracting(Historikkinnslag::getTittel).containsExactly("Revurdering opprettet");
    }

    @Test
    public void skal_opprette_kontrollbehandling_fra_siste_vedtak_mens_ordinær_behandling_er_åpen() {
        var revurderingTjeneste = lagRevurderingTjeneste(true);
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();
        var åpenRevurdering = revurderingTjeneste.opprettAutomatiskRevurdering(sisteVedtak, BehandlingÅrsakType.RE_ANNET);

        var kontrollbehandling = revurderingTjeneste.opprettKontrollbehandling(sisteVedtak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, false);

        assertThat(kontrollbehandling.getType()).isEqualTo(BehandlingType.KONTROLL);
        assertThat(kontrollbehandling.getId()).isNotEqualTo(åpenRevurdering.getId());
        assertThat(kontrollbehandling.getFagsakId()).isEqualTo(sisteVedtak.getFagsakId());
        assertThat(kontrollbehandling.getOriginalBehandlingId()).contains(sisteVedtak.getId());
        assertThat(kontrollbehandling.getBehandlingÅrsaker()).extracting(a -> a.getBehandlingÅrsakType())
            .containsExactly(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);
        assertThat(repositoryProvider.getVilkårResultatRepository().hent(kontrollbehandling.getId()).getVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET)).isPresent();
        assertThat(behandlingAnsvarligRepository.hentBehandlingAnsvarlig(kontrollbehandling.getId(), BehandlingDel.SENTRAL)).isPresent();
        assertThat(historikkRepository.hent(kontrollbehandling.getId())).extracting(Historikkinnslag::getTittel)
            .containsExactly("Kontrollbehandling opprettet");
    }

    @Test
    public void skal_ikke_opprette_kontrollbehandling_når_bryteren_er_av() {
        var revurderingTjeneste = lagRevurderingTjeneste(false);
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();

        assertThatThrownBy(() -> revurderingTjeneste.opprettKontrollbehandling(sisteVedtak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, false))
            .isInstanceOf(IllegalStateException.class);
        assertThat(repositoryProvider.getBehandlingRepository().hentAbsoluttAlleBehandlingerForFagsak(sisteVedtak.getFagsakId()))
            .noneMatch(Behandling::erKontrollbehandling);
    }

    @Test
    public void skal_ikke_opprette_ny_kontrollbehandling_når_fagsaken_har_kontrollbehandling_som_ikke_er_avsluttet() {
        var revurderingTjeneste = lagRevurderingTjeneste(true);
        var sisteVedtak = lagAvsluttetBehandlingMedVedtak();
        revurderingTjeneste.opprettKontrollbehandling(sisteVedtak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, false);

        assertThat(revurderingTjeneste.kanKontrollbehandlingOpprettes(sisteVedtak.getFagsak())).isFalse();
        assertThatThrownBy(() -> revurderingTjeneste.opprettKontrollbehandling(sisteVedtak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("kontrollbehandling som ikke er avsluttet");
    }

    @Test
    public void skal_ikke_opprette_kontrollbehandling_fra_behandling_som_ikke_er_siste_vedtak() {
        var revurderingTjeneste = lagRevurderingTjeneste(true);
        var eldreVedtak = lagAvsluttetBehandlingMedVedtak();
        var nyereVedtak = revurderingTjeneste.opprettAutomatiskRevurdering(eldreVedtak, BehandlingÅrsakType.RE_ANNET);
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(nyereVedtak);
        repositoryProvider.getBehandlingVedtakRepository().lagre(BehandlingVedtak.builder(nyereVedtak.getId())
            .medVedtakstidspunkt(LocalDateTime.now())
            .medAnsvarligSaksbehandler("Nav Navesen")
            .build(), lås);
        revurderingTestUtil.avsluttBehandling(nyereVedtak);

        assertThatThrownBy(() -> revurderingTjeneste.opprettKontrollbehandling(eldreVedtak, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("siste vedtak");
    }

    private Behandling lagAvsluttetBehandlingMedVedtak() {
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE);
        scenario.leggTilVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET, Utfall.OPPFYLT);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.lagre(repositoryProvider);
        revurderingTestUtil.avsluttBehandling(behandling);
        return behandling;
    }

    private RevurderingTjeneste lagRevurderingTjeneste(boolean kontrollbehandlingPå) {
        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        var behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
        when(behandlendeEnhetTjeneste.finnBehandlendeEnheterFor(any())).thenReturn(Map.of(BehandlingDel.SENTRAL, new OrganisasjonsEnhet("4863", "Nav test")));
        return new RevurderingTjeneste(behandlingskontrollTjeneste, revurderingTjenesteFelles, grunnlagKopierer, historikkRepository,
            prosessTriggereRepository, behandlingAnsvarligRepository, behandlendeEnhetTjeneste,
            fagsakLåsRepository, new KontrollbehandlingBryter(kontrollbehandlingPå, kontrollbehandlingPå));
    }

    private void opprettRevurderingsKandidat() {

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.lagre(repositoryProvider);
    }
}
