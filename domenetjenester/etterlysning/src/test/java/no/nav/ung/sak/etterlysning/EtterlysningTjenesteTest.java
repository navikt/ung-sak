package no.nav.ung.sak.etterlysning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class EtterlysningTjenesteTest {

    @Inject
    private MottatteDokumentRepository mottatteDokumentRepository;
    @Inject
    private EtterlysningRepository etterlysningRepository;

    @Inject
    private EntityManager entityManager;
    private Behandling behandling;

    private EtterlysningTjeneste etterlysningTjeneste;


    @BeforeEach
    void setUp() {
        behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        etterlysningTjeneste = new EtterlysningTjeneste(
            mottatteDokumentRepository,
            etterlysningRepository);
    }

    @Test
    void skal_finne_en_etterlysning_på_vent() {
        // Arrange
        final var etterlysning = lagEtterlysningPåVent(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));
        etterlysningRepository.lagre(etterlysning);

        // Act
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandling.getId(), behandling.getFagsakId(), EtterlysningType.UTTALELSE_ENDRET_STARTDATO);

        // Assert
        assertThat(gjeldendeEtterlysninger.size()).isEqualTo(1);
    }

    @Test
    void skal_finne_to_etterlysninger_på_vent_for_periode_kant_i_kant() {
        // Arrange
        final var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now());
        final var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(periode1.getTomDato().plusDays(1), periode1.getTomDato().plusDays(1));

        final var etterlysning1 = lagEtterlysningPåVent(periode1);
        final var etterlysning2 = lagEtterlysningPåVent(periode2);
        etterlysningRepository.lagre(List.of(etterlysning1, etterlysning2));

        // Act
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandling.getId(), behandling.getFagsakId(), EtterlysningType.UTTALELSE_ENDRET_STARTDATO);

        // Assert
        assertThat(gjeldendeEtterlysninger.size()).isEqualTo(2);
    }

    @Test
    void skal_finne_en_utløpt_etterlysning_for_to_delvis_overlappende_etterlysninger() {
        // Arrange
        final var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(3));
        final var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(periode1.getTomDato(), periode1.getTomDato().plusDays(1));

        final var etterlysning1 = lagUtløptEtterlysning(periode1);
        etterlysningRepository.lagre(etterlysning1);
        final var etterlysning2 = lagUtløptEtterlysning(periode2);
        etterlysningRepository.lagre(etterlysning2);

        // Act
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandling.getId(), behandling.getFagsakId(), EtterlysningType.UTTALELSE_ENDRET_STARTDATO);

        // Assert
        assertThat(gjeldendeEtterlysninger.size()).isEqualTo(1);
        final var faktisk = gjeldendeEtterlysninger.get(0);
        assertThat(faktisk.getPeriode()).isEqualTo(periode2);
    }

    @Test
    void skal_finne_en_utløpt_etterlysning_for_tre_delvis_overlappende_etterlysninger() {
        // Arrange
        final var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(3));
        final var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(periode1.getTomDato(), periode1.getTomDato().plusDays(3));
        final var periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(periode2.getTomDato(), periode2.getTomDato().plusDays(3));

        final var etterlysning1 = lagUtløptEtterlysning(periode1);
        etterlysningRepository.lagre(etterlysning1);
        final var etterlysning2 = lagUtløptEtterlysning(periode2);
        etterlysningRepository.lagre(etterlysning2);
        final var etterlysning3 = lagUtløptEtterlysning(periode3);
        etterlysningRepository.lagre(etterlysning3);

        // Act
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandling.getId(), behandling.getFagsakId(), EtterlysningType.UTTALELSE_ENDRET_STARTDATO);

        // Assert
        assertThat(gjeldendeEtterlysninger.size()).isEqualTo(1);
        final var faktisk = gjeldendeEtterlysninger.get(0);
        assertThat(faktisk.getPeriode()).isEqualTo(periode3);
    }

    @Test
    void skal_finne_to_utløpte_etterlysninger_for_to_ikke_overlappende_perioder_med_to_delvis_overlappende_etterlysninger_i_første_periode() {
        // Arrange
        final var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(3));
        final var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(periode1.getTomDato(), periode1.getTomDato().plusDays(3));
        final var periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(periode2.getTomDato().plusDays(2), periode2.getTomDato().plusDays(4));

        final var etterlysning1 = lagUtløptEtterlysning(periode1);
        etterlysningRepository.lagre(etterlysning1);
        final var etterlysning2 = lagUtløptEtterlysning(periode2);
        etterlysningRepository.lagre(etterlysning2);
        final var etterlysning3 = lagUtløptEtterlysning(periode3);
        etterlysningRepository.lagre(etterlysning3);

        // Act
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandling.getId(), behandling.getFagsakId(), EtterlysningType.UTTALELSE_ENDRET_STARTDATO);

        // Assert
        assertThat(gjeldendeEtterlysninger.size()).isEqualTo(2);
        final var faktisk1 = gjeldendeEtterlysninger.get(0);
        assertThat(faktisk1.getPeriode()).isEqualTo(periode2);
        final var faktisk2 = gjeldendeEtterlysninger.get(1);
        assertThat(faktisk2.getPeriode()).isEqualTo(periode3);
    }

    @Test
    void skal_finne_en_etterlysning_med_mottatt_svar_for_tre_delvis_overlappende_etterlysninger_med_to_utløpte() {
        // Arrange
        final var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(3));
        final var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(periode1.getTomDato(), periode1.getTomDato().plusDays(3));
        final var periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(periode2.getTomDato(), periode2.getTomDato().plusDays(3));

        final var etterlysning1 = lagUtløptEtterlysning(periode1);
        etterlysningRepository.lagre(etterlysning1);
        final var etterlysning2 = lagUtløptEtterlysning(periode2);
        etterlysningRepository.lagre(etterlysning2);

        final var svarJournalpostId = new JournalpostId(2L);
        opprettMottattDokument(svarJournalpostId, LocalDateTime.now());
        final var etterlysning3 = lagEtterlysningMedSvar(periode3, svarJournalpostId);
        etterlysningRepository.lagre(etterlysning3);

        // Act
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandling.getId(), behandling.getFagsakId(), EtterlysningType.UTTALELSE_ENDRET_STARTDATO);

        // Assert
        assertThat(gjeldendeEtterlysninger.size()).isEqualTo(1);
        final var faktisk = gjeldendeEtterlysninger.get(0);
        assertThat(faktisk.getPeriode()).isEqualTo(periode3);
    }


    @Test
    void skal_finne_en_etterlysning_med_mottatt_svar_for_tre_delvis_overlappende_etterlysninger() {
        // Arrange
        final var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(3));
        final var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(periode1.getTomDato(), periode1.getTomDato().plusDays(3));
        final var periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(periode2.getTomDato(), periode2.getTomDato().plusDays(3));

        final var svarJournalpostId1 = new JournalpostId(2L);
        opprettMottattDokument(svarJournalpostId1, LocalDateTime.now().minusDays(2));
        final var etterlysning1 = lagEtterlysningMedSvar(periode1, svarJournalpostId1);
        etterlysningRepository.lagre(etterlysning1);

        final var svarJournalpostId2 = new JournalpostId(3L);
        opprettMottattDokument(svarJournalpostId2, LocalDateTime.now());
        final var etterlysning2 = lagEtterlysningMedSvar(periode2, svarJournalpostId2);
        etterlysningRepository.lagre(etterlysning2);

        final var svarJournalpostId = new JournalpostId(4L);
        opprettMottattDokument(svarJournalpostId, LocalDateTime.now().minusDays(1));
        final var etterlysning3 = lagEtterlysningMedSvar(periode3, svarJournalpostId);
        etterlysningRepository.lagre(etterlysning3);

        // Act
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandling.getId(), behandling.getFagsakId(), EtterlysningType.UTTALELSE_ENDRET_STARTDATO);

        // Assert
        assertThat(gjeldendeEtterlysninger.size()).isEqualTo(1);
        final var faktisk = gjeldendeEtterlysninger.get(0);
        assertThat(faktisk.getPeriode()).isEqualTo(periode2);
    }

    private void opprettMottattDokument(JournalpostId svarJournalpostId, LocalDateTime innsendingstidspunkt) {
        mottatteDokumentRepository.lagre(new MottattDokument.Builder()
            .medFagsakId(behandling.getFagsakId())
            .medJournalPostId(svarJournalpostId)
            .medInnsendingstidspunkt(innsendingstidspunkt)
            .build(), DokumentStatus.GYLDIG);
    }


    private Etterlysning lagEtterlysningPåVent(DatoIntervallEntitet periode) {
        final var etterlysning = Etterlysning.opprettForType(behandling.getId(), UUID.randomUUID(), UUID.randomUUID(),
            periode,
            EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
        etterlysning.vent(LocalDateTime.now());
        return etterlysning;
    }

    private Etterlysning lagUtløptEtterlysning(DatoIntervallEntitet periode) {
        final var etterlysning = Etterlysning.opprettForType(behandling.getId(), UUID.randomUUID(), UUID.randomUUID(),
            periode,
            EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
        etterlysning.vent(LocalDateTime.now());
        etterlysning.utløpt();
        return etterlysning;
    }

    private Etterlysning lagEtterlysningMedSvar(DatoIntervallEntitet periode, JournalpostId svarJournalpostId) {
        final var etterlysning = Etterlysning.opprettForType(behandling.getId(), UUID.randomUUID(), UUID.randomUUID(),
            periode,
            EtterlysningType.UTTALELSE_ENDRET_STARTDATO);
        etterlysning.vent(LocalDateTime.now());
        etterlysning.mottaSvar(svarJournalpostId, false, "Uttalelse");
        return etterlysning;
    }
}
