package no.nav.foreldrepenger.behandling.revurdering;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BehandlingRepositoryFinnKandidaterTilRevurderingImplTest {

    private final static int revurderingDagerTilbake = 0;
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    private EtterkontrollRepository etterkontrollRepository;

    private Behandling behandling;

    @Test
    public void skal_finne_kandidat_til_revurdering() {
        Behandling behandling = opprettRevurderingsKandidat();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake))
            .medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        final List<Behandling> behandlings = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(behandlings).hasSize(1);
        assertThat(behandlings.get(0).getId()).isEqualTo(behandling.getId());
    }

    @Test
    public void behandling_som_har_vært_etterkontrollert_skal_ikke_være_kandidat_til_revurdering() {
        Behandling behandling = opprettRevurderingsKandidat();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake))
            .medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);

        final List<Behandling> behandlings = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(behandlings).isEmpty();
    }

    @Test
    public void skal_ikke_velge_henlagt_behandling() {
        Behandling behandling = opprettRevurderingsKandidat();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake))
            .medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        Behandling henlagtBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        henlagtBehandling.setBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET);
        henlagtBehandling.avsluttBehandling();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        final List<Behandling> behandlings = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P0D"));

        assertThat(behandlings).hasSize(1);
        assertThat(behandlings.get(0).getId()).isEqualTo(behandling.getId());
    }

    @Test
    public void fagsak_som_har_eksisterende_etterkontrollsbehandling_skal_ikke_være_kandidat_til_revurdering() {
        Behandling behandling = opprettRevurderingsKandidat();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake))
            .medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN)).build();

        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);

        List<Behandling> fagsakList = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(fagsakList).isEmpty();
    }

    @Test
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
            .medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake)).medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        List<Behandling> fagsakList = etterkontrollRepository
            .finnKandidaterForAutomatiskEtterkontroll(Period.parse("P" + revurderingDagerTilbake + "D"));

        assertThat(fagsakList).containsOnly(revurderingsBehandling);
    }

    @Test
    public void behandling_med_nyere_termindato_skal_ikke_være_kandidat_til_revurdering() {
        opprettRevurderingsKandidat();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false).medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(revurderingDagerTilbake))
            .medKontrollType(KontrollType.MANGLENDE_FØDSEL).build();
        etterkontrollRepository.lagre(etterkontroll);

        List<Behandling> fagsakList = etterkontrollRepository
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
