package no.nav.k9.sak.domene.risikoklassifisering.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.risikoklassifisering.FaresignalVurdering;
import no.nav.k9.kodeverk.risikoklassifisering.Kontrollresultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class RisikoklassifiseringRepositoryImplTest {

    @Inject
    private EntityManager entityManager;

    private RisikoklassifiseringRepository risikorepository;
    private Repository repository ;
    private BehandlingRepositoryProvider repositoryProvider ;
    private BehandlingRepository behandlingRepository ;

    @BeforeEach
    public void setup(){
        risikorepository = new RisikoklassifiseringRepository(entityManager);
        repository = new Repository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Test
    public void skal_lagre_og_hente_klassifisering() {
        // Arrange
        Behandling behandling = opprettBehandling();
        RisikoklassifiseringEntitet risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.IKKE_KLASSIFISERT);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);
    }

    @Test
    public void skal_ikke_hente_klassifisering_gitt_ugyldig_behandlingId() {
        // Act
        Optional<RisikoklassifiseringEntitet> risikoklassifiseringEntitet = risikorepository.hentRisikoklassifiseringForBehandling(123);

        // Assert
        assertThat(risikoklassifiseringEntitet).isNotPresent();
    }

    @Test
    public void skal_deakivere_gammelt_grunnlag_når_det_eksiterer() {
        // Arrange
        Behandling behandling = opprettBehandling();
        RisikoklassifiseringEntitet risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.IKKE_KLASSIFISERT);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);

        // Arrange
        RisikoklassifiseringEntitet nyRisikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.HØY);

        // Act
        risikorepository.lagreRisikoklassifisering(nyRisikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> nyPersistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(nyPersistertKlassifisering).isPresent();
        assertThat(nyPersistertKlassifisering.get()).isEqualTo(nyRisikoklassifiseringEntitet);
    }

    @Test
    public void skal_oppdatere_klassifisering_med_vurdering_fra_saksbehandler_når_gammel_klassifisering_ikke_var_vurdert() {
        // Arrange
        Behandling behandling = opprettBehandling();
        RisikoklassifiseringEntitet risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.HØY);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);

        // Act
        risikorepository.lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering.INNVIRKNING, behandling.getId());
        Optional<RisikoklassifiseringEntitet> nyPersistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(nyPersistertKlassifisering).isPresent();
        RisikoklassifiseringEntitet entitet = nyPersistertKlassifisering.get();
        assertThat(entitet.getKontrollresultat()).isEqualTo(risikoklassifiseringEntitet.getKontrollresultat());
        assertThat(entitet.getBehandlingId()).isEqualTo(risikoklassifiseringEntitet.getBehandlingId());
        assertThat(entitet.getFaresignalVurdering()).isEqualTo(FaresignalVurdering.INNVIRKNING);
    }

    @Test
    public void skal_oppdatere_klassifisering_med_vurdering_fra_saksbehandler_når_gammel_klassifisering_var_vurdert() {
        // Arrange
        Behandling behandling = opprettBehandling();
        RisikoklassifiseringEntitet risikoklassifiseringEntitet = lagRisikoklassifisering(behandling.getId(), Kontrollresultat.HØY, FaresignalVurdering.INNVIRKNING);

        // Act
        risikorepository.lagreRisikoklassifisering(risikoklassifiseringEntitet, behandling.getId());
        Optional<RisikoklassifiseringEntitet> persistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(persistertKlassifisering).isPresent();
        assertThat(persistertKlassifisering.get()).isEqualTo(risikoklassifiseringEntitet);

        // Act
        risikorepository.lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering.INGEN_INNVIRKNING, behandling.getId());
        Optional<RisikoklassifiseringEntitet> nyPersistertKlassifisering = risikorepository.hentRisikoklassifiseringForBehandling(behandling.getId());

        // Assert
        assertThat(nyPersistertKlassifisering).isPresent();
        RisikoklassifiseringEntitet entitet = nyPersistertKlassifisering.get();
        assertThat(entitet.getKontrollresultat()).isEqualTo(risikoklassifiseringEntitet.getKontrollresultat());
        assertThat(entitet.getBehandlingId()).isEqualTo(risikoklassifiseringEntitet.getBehandlingId());
        assertThat(entitet.getFaresignalVurdering()).isEqualTo(FaresignalVurdering.INGEN_INNVIRKNING);
    }

    @Test
    public void skal_feile_under_oppdatering_når_gammelt_grunnlag_ikke_finnes() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            // Arrange
            Behandling behandling = opprettBehandling();

            // Act
            risikorepository.lagreVurderingAvFaresignalerForRisikoklassifisering(FaresignalVurdering.INGEN_INNVIRKNING, behandling.getId());
        });
    }

    private RisikoklassifiseringEntitet lagRisikoklassifisering(Long behandlingId, Kontrollresultat kontrollresultat, FaresignalVurdering faresignalvurdering) {
        return RisikoklassifiseringEntitet.builder().medKontrollresultat(kontrollresultat).medFaresignalVurdering(faresignalvurdering).buildFor(behandlingId);
    }

    private RisikoklassifiseringEntitet lagRisikoklassifisering(Long behandlingId, Kontrollresultat kontrollresultat) {
        return RisikoklassifiseringEntitet.builder().medKontrollresultat(kontrollresultat).buildFor(behandlingId);
    }

    private Behandling opprettBehandling() {
        Fagsak fagsak = opprettFagsak();
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        return behandling;
    }

    private Fagsak opprettFagsak() {
        // Opprett fagsak
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, AktørId.dummy(), new Saksnummer("1000"));
        repository.lagre(fagsak);
        repository.flush();
        return fagsak;
    }

}
