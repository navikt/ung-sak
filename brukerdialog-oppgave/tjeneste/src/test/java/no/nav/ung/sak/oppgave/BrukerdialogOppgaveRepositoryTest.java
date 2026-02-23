package no.nav.ung.sak.oppgave;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveStatus;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.oppgave.typer.oppgave.søkytelse.SøkYtelseOppgaveDataEntitet;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester for BrukerdialogOppgaveRepository.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrukerdialogOppgaveRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private BrukerdialogOppgaveRepository repository;

    private AktørId aktørId;

    @BeforeEach
    void setUp() {
        aktørId = new AktørId("1234567890123");
    }

    @Test
    void skal_persistere_og_hente_oppgave() {
        // Arrange
        UUID oppgaveReferanse = UUID.randomUUID();
        LocalDate fomDato = LocalDate.of(2026, 2, 1);

        BrukerdialogOppgaveEntitet oppgave = new BrukerdialogOppgaveEntitet(
            oppgaveReferanse,
            OppgaveType.SØK_YTELSE,
            aktørId,
            null
        );

        // Act
        repository.persister(oppgave);
        var oppgaveData = new SøkYtelseOppgaveDataEntitet(fomDato);
        oppgave.setOppgaveData(oppgaveData);
        entityManager.persist(oppgaveData);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<BrukerdialogOppgaveEntitet> hentetOppgave = repository.hentOppgaveForOppgavereferanse(oppgaveReferanse, aktørId);

        assertThat(hentetOppgave).isPresent();
        assertThat(hentetOppgave.get().getOppgavereferanse()).isEqualTo(oppgaveReferanse);
        assertThat(hentetOppgave.get().getOppgaveType()).isEqualTo(OppgaveType.SØK_YTELSE);
        assertThat(hentetOppgave.get().getAktørId()).isEqualTo(aktørId);
        assertThat(hentetOppgave.get().getStatus()).isEqualTo(OppgaveStatus.ULØST);
        assertThat(hentetOppgave.get().getOppgaveData()).isInstanceOf(SøkYtelseOppgaveDataEntitet.class);
    }

    @Test
    void skal_hente_alle_oppgaver_for_aktør() {
        // Arrange
        opprettOppgave(aktørId, OppgaveType.SØK_YTELSE);
        opprettOppgave(aktørId, OppgaveType.RAPPORTER_INNTEKT);
        opprettOppgave(new AktørId("9876543210987"), OppgaveType.SØK_YTELSE); // Annen aktør

        entityManager.flush();
        entityManager.clear();

        // Act
        List<BrukerdialogOppgaveEntitet> oppgaver = repository.hentAlleOppgaverForAktør(aktørId);

        // Assert
        assertThat(oppgaver).hasSize(2);
        assertThat(oppgaver).allMatch(o -> o.getAktørId().equals(aktørId));
    }

    @Test
    void skal_oppdatere_oppgave_til_lukket() {
        // Arrange
        BrukerdialogOppgaveEntitet oppgave = opprettOppgave(aktørId, OppgaveType.SØK_YTELSE);
        UUID oppgaveReferanse = oppgave.getOppgavereferanse();

        entityManager.flush();
        entityManager.clear();

        // Act
        BrukerdialogOppgaveEntitet hentetOppgave = repository.hentOppgaveForOppgavereferanse(oppgaveReferanse, aktørId).get();
        repository.lukkOppgave(hentetOppgave);

        entityManager.flush();
        entityManager.clear();

        // Assert
        BrukerdialogOppgaveEntitet verifisertOppgave = repository.hentOppgaveForOppgavereferanse(oppgaveReferanse, aktørId).get();
        assertThat(verifisertOppgave.getStatus()).isEqualTo(OppgaveStatus.LUKKET);
        assertThat(verifisertOppgave.getLukketDato()).isNotNull();
    }

    @Test
    void skal_oppdatere_oppgave_til_åpnet() {
        // Arrange
        BrukerdialogOppgaveEntitet oppgave = opprettOppgave(aktørId, OppgaveType.SØK_YTELSE);
        UUID oppgaveReferanse = oppgave.getOppgavereferanse();

        entityManager.flush();
        entityManager.clear();

        // Act
        BrukerdialogOppgaveEntitet hentetOppgave = repository.hentOppgaveForOppgavereferanse(oppgaveReferanse, aktørId).get();
        repository.åpneOppgave(hentetOppgave);

        entityManager.flush();
        entityManager.clear();

        // Assert
        BrukerdialogOppgaveEntitet verifisertOppgave = repository.hentOppgaveForOppgavereferanse(oppgaveReferanse, aktørId).get();
        assertThat(verifisertOppgave.getÅpnetDato()).isNotNull();
    }

    @Test
    void skal_hente_oppgaver_basert_på_type_og_status() {
        // Arrange
        BrukerdialogOppgaveEntitet oppgave1 = opprettOppgave(aktørId, OppgaveType.SØK_YTELSE);
        opprettOppgave(aktørId, OppgaveType.RAPPORTER_INNTEKT);
        BrukerdialogOppgaveEntitet oppgave3 = opprettOppgave(aktørId, OppgaveType.SØK_YTELSE);

        // Løs én av SØK_YTELSE oppgavene
        repository.lukkOppgave(oppgave3);

        entityManager.flush();
        entityManager.clear();

        // Act
        List<BrukerdialogOppgaveEntitet> uløstesøkYtelseOppgaver =
            repository.hentOppgaveForType(OppgaveType.SØK_YTELSE, OppgaveStatus.ULØST, aktørId);

        // Assert
        assertThat(uløstesøkYtelseOppgaver).hasSize(1);
        assertThat(uløstesøkYtelseOppgaver.get(0).getOppgavereferanse()).isEqualTo(oppgave1.getOppgavereferanse());
    }

    @Test
    void skal_returnere_empty_når_oppgave_ikke_finnes() {
        // Act
        Optional<BrukerdialogOppgaveEntitet> oppgave =
            repository.hentOppgaveForOppgavereferanse(UUID.randomUUID(), aktørId);

        // Assert
        assertThat(oppgave).isEmpty();
    }

    @Test
    void skal_ikke_hente_oppgave_for_feil_aktør() {
        // Arrange
        BrukerdialogOppgaveEntitet oppgave = opprettOppgave(aktørId, OppgaveType.SØK_YTELSE);
        UUID oppgaveReferanse = oppgave.getOppgavereferanse();
        AktørId annenAktørId = new AktørId("9876543210987");

        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<BrukerdialogOppgaveEntitet> hentetOppgave =
            repository.hentOppgaveForOppgavereferanse(oppgaveReferanse, annenAktørId);

        // Assert
        assertThat(hentetOppgave).isEmpty();
    }

    // Hjelpemetode for å opprette testoppgaver
    private BrukerdialogOppgaveEntitet opprettOppgave(AktørId aktørId, OppgaveType type) {
        UUID oppgaveReferanse = UUID.randomUUID();

        BrukerdialogOppgaveEntitet oppgave = new BrukerdialogOppgaveEntitet(
            oppgaveReferanse,
            type,
            aktørId,
            null
        );

        repository.persister(oppgave);
        var oppgaveData = new SøkYtelseOppgaveDataEntitet(LocalDate.now());
        oppgave.setOppgaveData(oppgaveData);
        entityManager.persist(oppgaveData);
        return oppgave;
    }
}
