package no.nav.k9.sak.behandling.revurdering.etterkontroll;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class EtterkontrollRepositoryFinnKandidaterTilRevurderingImplTest {

    private final static int revurderingDagerTilbake = 0;

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    private EtterkontrollRepository etterkontrollRepository;

    private Behandling behandling;

    @BeforeEach
    public void before() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    public void skal_finne_kandidat_til_revurdering() {
        Behandling behandling = opprettRevurderingsKandidat();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake))
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();
        etterkontrollRepository.lagre(etterkontroll);

        final List<Etterkontroll> etterkontroller = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(etterkontroller).hasSize(1);
        assertThat(etterkontroller.get(0).getFagsakId()).isEqualTo(behandling.getFagsakId());
    }

    @Test
    public void behandling_som_har_vært_etterkontrollert_skal_ikke_være_kandidat_til_revurdering() {
        Behandling behandling = opprettRevurderingsKandidat();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake))
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();
        etterkontrollRepository.lagre(etterkontroll);

        etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.FORSINKET_SAKSBEHANDLINGSTID);

        final List<Etterkontroll> etterkontroller = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(etterkontroller).isEmpty();
    }

    @Test
    @Deprecated //Overflødig?
    public void skal_hente_ut_siste_vedtak_til_revurdering() {
        Behandling behandling = opprettRevurderingsKandidat();

        Behandling.Builder revurderingBuilder = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ANNET));
        Behandling revurderingsBehandling = revurderingBuilder.build();
        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(revurderingsBehandling.getId())
            .medAnsvarligSaksbehandler("asdf").build();
        revurderingsBehandling.avsluttBehandling();
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(revurderingsBehandling));
        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        Etterkontroll etterkontroll = new Etterkontroll.Builder(revurderingsBehandling.getFagsakId()).medErBehandlet(false)
            .medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake)).medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();
        etterkontrollRepository.lagre(etterkontroll);

        List<Long> fagsakList = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"))
            .stream().map(Etterkontroll::getFagsakId)
            .toList();

        assertThat(fagsakList).containsOnly(revurderingsBehandling.getFagsakId());
    }

    @Test
    public void behandling_med_nyere_termindato_skal_ikke_være_kandidat_til_revurdering() {
        opprettRevurderingsKandidat();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake))
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID).build();
        etterkontrollRepository.lagre(etterkontroll);

        List<Etterkontroll> fagsakList = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P5D"));

        assertThat(fagsakList).isEmpty();
    }

    private Behandling opprettRevurderingsKandidat() {
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
            .medAnsvarligSaksbehandler("asdf");
        behandling = scenario.lagre(repositoryProvider);

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

    @Test
    public void skal_finne_nyeste_innvilgete_avsluttede_behandling_som_ikke_er_henlagt() {
        Behandling behandling = opprettRevurderingsKandidat();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        Behandling henlagtBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        henlagtBehandling.setBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET);
        henlagtBehandling.avsluttBehandling();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        Optional<Behandling> resultatOpt = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsak().getId());
        assertThat(resultatOpt).hasValueSatisfying(resultat -> assertThat(resultat.getId()).isEqualTo(behandling.getId()));
    }

}
