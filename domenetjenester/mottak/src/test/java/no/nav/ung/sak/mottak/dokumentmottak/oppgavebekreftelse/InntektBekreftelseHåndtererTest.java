package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse;
import no.nav.k9.søknad.felles.Versjon;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.SøknadId;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
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
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InntektBekreftelseHåndtererTest {

    @Inject
    private EntityManager em;
    private EtterlysningRepository etterlysningRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;


    @BeforeEach
    void setup() {
        etterlysningRepository = new EtterlysningRepository(em);
        mottatteDokumentRepository = new MottatteDokumentRepository(em);
    }

    @Test
    void skalOppdatereEtterlysningOgSetteBehandlingAvVent() {
        // Arrange
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING);
        Behandling behandling = scenarioBuilder.lagre(em);
        var periode = DatoIntervallEntitet.fra(LocalDate.now(), LocalDate.now());
        long journalpostId = 892L;
        var oppgaveId = UUID.randomUUID();
        var grunnlagsreferanse = UUID.randomUUID();

        final var mottattDokument = lagMottattDokument(behandling, journalpostId);

        var etterlysning = etterlysningRepository.lagre(Etterlysning.forInntektKontrollUttalelse(
            behandling.getId(),
            grunnlagsreferanse,
            oppgaveId,
            periode));

        etterlysning.vent(LocalDateTime.now().plusDays(1));
        etterlysningRepository.lagre(etterlysning);
        em.flush();

        var bekreftelse = lagBekreftelse(mottattDokument, behandling, new InntektBekreftelse(
            oppgaveId,
            false,
            null));

        // Act
        var inntektBekreftelseHåndterer = new GenerellOppgaveBekreftelseHåndterer(mottatteDokumentRepository, etterlysningRepository);
        inntektBekreftelseHåndterer.håndter(bekreftelse);
        em.flush();

        // Assert
        // Dokument er gyldig
        final var dokumentTilBehandling = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId(), DokumentStatus.GYLDIG);
        assertThat(dokumentTilBehandling).hasSize(1);

        //etterlysning er oppdatert
        var oppdatertEtterlysning = etterlysningRepository.hentEtterlysning(etterlysning.getId());
        assertThat(oppdatertEtterlysning.getStatus()).isEqualTo(EtterlysningStatus.MOTTATT_SVAR);
        assertThat(oppdatertEtterlysning.getUttalelse().get().getUttalelseBegrunnelse()).isNull();
        assertThat(oppdatertEtterlysning.getUttalelse().get().harUttalelse()).isFalse();
        assertThat(oppdatertEtterlysning.getUttalelse().get().getSvarJournalpostId().getJournalpostId().getVerdi()).isEqualTo(String.valueOf(journalpostId));
    }

    @Test
    void skal_håndtere_uttalelse() {
        // Arrange
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING);
        Behandling behandling = scenarioBuilder.lagre(em);
        var periode = DatoIntervallEntitet.fra(LocalDate.now(), LocalDate.now());
        long journalpostId = 892L;
        var oppgaveId = UUID.randomUUID();
        var grunnlagsreferanse = UUID.randomUUID();

        var etterlysning = etterlysningRepository.lagre(Etterlysning.forInntektKontrollUttalelse(
            behandling.getId(),
            grunnlagsreferanse,
            oppgaveId,
            periode));

        etterlysning.vent(LocalDateTime.now().plusDays(1));
        etterlysningRepository.lagre(etterlysning);
        em.flush();

        var bekreftelse = lagBekreftelse(lagMottattDokument(behandling, journalpostId), behandling, new InntektBekreftelse(
            oppgaveId,
            true,
            "en uttalelse"));

        // Act
        var inntektBekreftelseHåndterer = new GenerellOppgaveBekreftelseHåndterer(mottatteDokumentRepository, etterlysningRepository);
        inntektBekreftelseHåndterer.håndter(bekreftelse);
        em.flush();

        // Assert
        // Dokument er gyldig
        final var dokumentTilBehandling = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId(), DokumentStatus.GYLDIG);
        assertThat(dokumentTilBehandling).hasSize(1);

        //etterlysning er oppdatert
        var oppdatertEtterlysning = etterlysningRepository.hentEtterlysning(etterlysning.getId());
        assertThat(oppdatertEtterlysning.getStatus()).isEqualTo(EtterlysningStatus.MOTTATT_SVAR);
        assertThat(oppdatertEtterlysning.getUttalelse().get().getUttalelseBegrunnelse()).isEqualTo("en uttalelse");
        assertThat(oppdatertEtterlysning.getUttalelse().get().harUttalelse()).isTrue();
        assertThat(oppdatertEtterlysning.getUttalelse().get().getSvarJournalpostId().getJournalpostId().getVerdi()).isEqualTo(String.valueOf(journalpostId));
    }

    private MottattDokument lagMottattDokument(Behandling behandling, long journalpostId) {
        final var builder = new MottattDokument.Builder();
        final var mottattDokument = builder
            .medInnsendingstidspunkt(LocalDateTime.now())
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medJournalPostId(new JournalpostId(journalpostId))
            .build();
        mottatteDokumentRepository.lagre(mottattDokument, DokumentStatus.BEHANDLER);
        return mottattDokument;
    }

    private static OppgaveBekreftelseInnhold lagBekreftelse(MottattDokument mottattDokument, Behandling behandling, InntektBekreftelse inntektBekreftelse) {
        return new OppgaveBekreftelseInnhold(
            mottattDokument,
            behandling,
            new OppgaveBekreftelse(
                new SøknadId("456"),
                Versjon.of("1"),
                ZonedDateTime.now(),
                new Søker(NorskIdentitetsnummer.of("12345678910")),
                inntektBekreftelse
            ),
            Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE
        );
    }
}
