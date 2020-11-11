package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class OppgaveBehandlingKoblingRepositoryImplTest {

    private static final Saksnummer DUMMY_SAKSNUMMER = new Saksnummer("123");

    @Inject
    private EntityManager entityManager;

    private Repository repository ;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository ;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(entityManager);
    }

    @Test
    public void skal_hente_opp_oppgave_behandling_kobling_basert_på_oppgave_id() {
        // Arrange
        String oppgaveId = "G1502453";
        Behandling behandling = new BasicBehandlingBuilder(entityManager).opprettOgLagreFørstegangssøknad(FagsakYtelseType.ENGANGSTØNAD);
        lagOppgave(new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK_VL, oppgaveId, DUMMY_SAKSNUMMER, behandling));

        // Act
        Optional<OppgaveBehandlingKobling> behandlingKoblingOpt = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);

        // Assert
        assertThat(behandlingKoblingOpt).hasValueSatisfying(behandlingKobling ->
            assertThat(behandlingKobling.getOppgaveÅrsak()).isEqualTo(OppgaveÅrsak.BEHANDLE_SAK_VL)
        );
    }

    @Test
    public void skal_hente_opp_oppgave_behandling_koblinger_for_åpne_oppgaver() {
        // Arrange
        Behandling behandling = new BasicBehandlingBuilder(entityManager).opprettOgLagreFørstegangssøknad(FagsakYtelseType.ENGANGSTØNAD);
        OppgaveBehandlingKobling bsAvsl = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK_VL, "O1234", DUMMY_SAKSNUMMER, behandling);
        bsAvsl.ferdigstillOppgave("I11111");
        OppgaveBehandlingKobling bsAapen = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK_VL, "O1235", DUMMY_SAKSNUMMER, behandling);
        OppgaveBehandlingKobling godkjenn = new OppgaveBehandlingKobling(OppgaveÅrsak.GODKJENN_VEDTAK_VL, "O1236", DUMMY_SAKSNUMMER, behandling);
        OppgaveBehandlingKobling registrer = new OppgaveBehandlingKobling(OppgaveÅrsak.VURDER_DOKUMENT, "O1238", DUMMY_SAKSNUMMER, behandling);
        OppgaveBehandlingKobling revurder = new OppgaveBehandlingKobling(OppgaveÅrsak.REVURDER_VL, "O1237", DUMMY_SAKSNUMMER, behandling);

        lagOppgave(bsAapen);
        lagOppgave(bsAvsl);
        lagOppgave(godkjenn);
        lagOppgave(revurder);
        lagOppgave(registrer);

        // Act
        List<OppgaveBehandlingKobling> behandlingKobling = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(LocalDate.now(), LocalDate.now(), Set.of(OppgaveÅrsak.BEHANDLE_SAK_VL, OppgaveÅrsak.REVURDER_VL));

        // Assert
        assertThat(behandlingKobling).hasSize(2);

        // Change + reassert
        revurder.ferdigstillOppgave("I11111");
        lagOppgave(revurder);
        behandlingKobling = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(LocalDate.now(), LocalDate.now(), Set.of(OppgaveÅrsak.BEHANDLE_SAK_VL, OppgaveÅrsak.REVURDER_VL));
        assertThat(behandlingKobling).hasSize(1);

    }

    private void lagOppgave(OppgaveBehandlingKobling oppgaveBehandlingKobling) {
        oppgaveBehandlingKoblingRepository.lagre(oppgaveBehandlingKobling);
        repository.flush();
    }


}
