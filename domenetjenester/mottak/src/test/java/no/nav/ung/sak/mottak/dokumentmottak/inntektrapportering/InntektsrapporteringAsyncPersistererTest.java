package no.nav.ung.sak.mottak.dokumentmottak.inntektrapportering;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntekt;
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntektForPeriode;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InntektsrapporteringAsyncPersistererTest {

    @Mock
    private MottatteDokumentRepository mottatteDokumentRepository;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private InntektsrapporteringAsyncPersisterer persisterer;

    @BeforeEach
    void setUp() {
        persisterer = new InntektsrapporteringAsyncPersisterer(mottatteDokumentRepository, prosessTaskTjeneste);
    }

    @Test
    void tomInntekt_setterDokumentGyldigOgOppreterIkkeTask() {
        // Arrange
        Behandling behandling = lagMocketBehandling();
        MottattDokument dokument = lagMottattDokument();
        OppgittInntekt tomInntekt = OppgittInntekt.builder()
            .medOppgittePeriodeinntekter(Set.of(
                OppgittInntektForPeriode.builder(new Periode(LocalDate.now(), LocalDate.now().plusDays(6))).build()
            )).build();

        // Act
        persisterer.opprettTaskForPersistering(behandling, dokument, tomInntekt);

        // Assert
        verify(mottatteDokumentRepository).oppdaterStatus(List.of(dokument), DokumentStatus.GYLDIG);
        verifyNoInteractions(prosessTaskTjeneste);
        verify(mottatteDokumentRepository, never()).lagre(any(), any());
    }

    @Test
    void inntektMedData_lagrerTaskOgSetterDokumentTilBehandler() {
        // Arrange
        Behandling behandling = lagMocketBehandling();
        MottattDokument dokument = lagMottattDokument();
        OppgittInntekt inntekt = OppgittInntekt.builder()
            .medOppgittePeriodeinntekter(Set.of(
                OppgittInntektForPeriode.builder(new Periode(LocalDate.now(), LocalDate.now().plusDays(6)))
                    .medArbeidstakerOgFrilansinntekt(BigDecimal.TEN)
                    .build()
            )).build();

        // Act
        persisterer.opprettTaskForPersistering(behandling, dokument, inntekt);

        // Assert
        verify(mottatteDokumentRepository).lagre(dokument, DokumentStatus.BEHANDLER);
        verify(mottatteDokumentRepository, never()).oppdaterStatus(any(), any());

        ArgumentCaptor<ProsessTaskData> taskCaptor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(taskCaptor.capture());
        ProsessTaskData task = taskCaptor.getValue();
        assertThat(task.getPropertyValue(no.nav.ung.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask.JOURNALPOST_ID))
            .isEqualTo(dokument.getJournalpostId().getVerdi());
        assertThat(task.getPropertyValue(no.nav.ung.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask.BREVKODER))
            .isEqualTo(dokument.getType().getKode());
    }

    private Behandling lagMocketBehandling() {
        Fagsak fagsak = mock(Fagsak.class);
        when(fagsak.getSaksnummer()).thenReturn(new Saksnummer("SAKEN"));
        when(fagsak.getPeriode()).thenReturn(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusYears(1)));

        AktørId aktørId = AktørId.dummy();

        Behandling behandling = mock(Behandling.class);
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.UNGDOMSYTELSE);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandling.getBehandlingResultatType()).thenReturn(BehandlingResultatType.IKKE_FASTSATT);
        when(behandling.getAktørId()).thenReturn(aktørId);
        when(behandling.getFagsakId()).thenReturn(1L);
        when(behandling.getId()).thenReturn(2L);
        when(behandling.getUuid()).thenReturn(UUID.randomUUID());
        when(behandling.getOriginalBehandlingId()).thenReturn(Optional.empty());
        when(behandling.getStatus()).thenReturn(BehandlingStatus.UTREDES);

        return behandling;
    }

    private MottattDokument lagMottattDokument() {
        return new MottattDokument.Builder()
            .medFagsakId(1L)
            .medJournalPostId(new JournalpostId("123"))
            .medType(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING)
            .medStatus(DokumentStatus.BEHANDLER)
            .medInnsendingstidspunkt(LocalDateTime.now())
            .build();
    }
}
