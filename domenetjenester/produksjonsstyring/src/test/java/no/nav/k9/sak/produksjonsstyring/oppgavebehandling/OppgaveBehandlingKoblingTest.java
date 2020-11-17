package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.test.util.fagsak.FagsakBuilder;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class OppgaveBehandlingKoblingTest {
    private static final Saksnummer SAKSNUMMER = new Saksnummer("123");

    @Inject
    private EntityManager entityManager;

    private Repository repository ;
    private BehandlingRepositoryProvider repositoryProvider ;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository ;

    private Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).build();

    @BeforeEach
    public void setup() {

        repository = new Repository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(entityManager);

        repository.lagre(fagsak);
        repository.flush();
    }

    @Test
    public void skal_lagre_ned_en_oppgave() throws Exception {
        // Arrange
        String oppgaveIdFraGSAK = "IDFRAGSAK";
        OppgaveÅrsak behandleSøknad = OppgaveÅrsak.BEHANDLE_SAK_VL;

        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(behandleSøknad, oppgaveIdFraGSAK, SAKSNUMMER, behandling);
        long id = lagreOppgave(oppgave);

        // Assert
        OppgaveBehandlingKobling oppgaveFraBase = repository.hent(OppgaveBehandlingKobling.class, id);
        assertThat(oppgaveFraBase.getOppgaveId()).isEqualTo(oppgaveIdFraGSAK);
    }

    private long lagreOppgave(OppgaveBehandlingKobling oppgave) {
        return oppgaveBehandlingKoblingRepository.lagre(oppgave);
    }

    @Test
    public void skal_knytte_en_oppgave_til_en_behandling() throws Exception {
        // Arrange
        String oppgaveIdFraGSAK = "IDFRAGSAK";
        OppgaveÅrsak behandleSøknad = OppgaveÅrsak.BEHANDLE_SAK_VL;

        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(behandleSøknad, oppgaveIdFraGSAK, SAKSNUMMER, behandling);
        lagreOppgave(oppgave);

        // Assert
        List<Behandling> behandlinger = repository.hentAlle(Behandling.class);
        assertThat(behandlinger).hasSize(1);
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlinger.get(0).getId());
        assertThat(OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.BEHANDLE_SAK_VL, oppgaveBehandlingKoblinger)).isNotNull();
    }

    @Test
    public void skal_kunne_ferdigstille_en_eksisterende_oppgave() throws Exception {
        // Arrange
        String oppgaveIdFraGSAK = "IDFRAGSAK";
        OppgaveÅrsak behandleSøknad = OppgaveÅrsak.BEHANDLE_SAK_VL  ;
        String saksbehandler = "R160223";

        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);

        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(behandleSøknad, oppgaveIdFraGSAK, SAKSNUMMER, behandling);
        Long id = lagreOppgave(oppgave);

        // Act
        OppgaveBehandlingKobling oppgaveFraBase = repository.hent(OppgaveBehandlingKobling.class, id);
        oppgaveFraBase.ferdigstillOppgave(saksbehandler);
        lagreOppgave(oppgaveFraBase);

        OppgaveBehandlingKobling oppgaveHentetFraBasen = repository.hent(OppgaveBehandlingKobling.class, oppgaveFraBase.getId());
        assertThat(oppgaveHentetFraBasen.isFerdigstilt()).isTrue();
    }

}
