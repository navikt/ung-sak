package no.nav.k9.sak.behandlingslager.behandling.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class SøknadRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    private DatoIntervallEntitet søknadsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1), LocalDate.now().minusDays(1));

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        søknadRepository = repositoryProvider.getSøknadRepository();
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Test
    public void skal_finne_endringssøknad_for_behandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        Behandling behandling2 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling2, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling2));

        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling, søknad);

        SøknadEntitet søknad2 = opprettSøknad(true);
        søknadRepository.lagreOgFlush(behandling2, søknad2);

        // Act
        Optional<SøknadEntitet> endringssøknad = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling.getId());
        Optional<SøknadEntitet> endringssøknad2 = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling2.getId());

        // Assert
        assertThat(endringssøknad).isPresent();
        assertThat(endringssøknad2).isPresent();
        assertThat(endringssøknad.get()).isNotEqualTo(endringssøknad2.get());
    }

    @Test
    public void skal_finne_søknad_med_overlapp() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling, søknad);

        var dato = søknadsperiode.getTomDato().minusDays(1);
        List<Behandling> behandlinger = søknadRepository.hentBehandlingerMedOverlappendeSøknaderIPeriode(fagsak.getId(), dato, dato);

        assertThat(behandlinger).containsOnly(behandling);

    }

    @Test
    public void skal_ikke_finne_endringssøknad_for_behandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling, søknad);

        // Act
        Optional<SøknadEntitet> endringssøknad = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling.getId());

        // Assert
        assertThat(endringssøknad).isPresent();
        assertThat(endringssøknad.get().erEndringssøknad()).isFalse();
    }

    @Test
    public void skal_kopiere_søknadsgrunnlaget_fra_behandling1_til_behandling2() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AktørId.dummy());
        fagsakRepository.opprettNy(fagsak);

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling1, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling1));
        SøknadEntitet søknad = opprettSøknad(false);
        søknadRepository.lagreOgFlush(behandling1, søknad);

        Behandling behandling2 = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling2, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling2));

        // Act
        søknadRepository.kopierGrunnlagFraEksisterendeBehandling(behandling1, behandling2);

        // Assert
        Optional<SøknadEntitet> søknadEntitet = søknadRepository.hentSøknadHvisEksisterer(behandling2.getId());
        assertThat(søknadEntitet).isPresent();
    }

    private SøknadEntitet opprettSøknad(boolean erEndringssøknad) {
        return new SøknadEntitet.Builder()
            .medSøknadsperiode(søknadsperiode)
            .medSøknadsdato(søknadsperiode.getTomDato().plusDays(1))
            .medErEndringssøknad(erEndringssøknad)
            .build();
    }
}
