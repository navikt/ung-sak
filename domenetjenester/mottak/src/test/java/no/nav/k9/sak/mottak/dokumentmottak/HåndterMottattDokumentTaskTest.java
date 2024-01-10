package no.nav.k9.sak.mottak.dokumentmottak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@CdiDbAwareTest
@ExtendWith(MockitoExtension.class)
class HåndterMottattDokumentTaskTest {

    @Inject
    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    @Inject
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    @Mock
    private DokumentValidatorProvider dokumentValidatorProvider;
    @Mock
    private DokumentValidator dokumentValidator;
    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Inject
    private EntityManager entityManager;

    private Fagsak fagsak;
    private Behandling behandling;

    @BeforeEach
    void setup() {
        Long fagsakId = fagsakRepository.opprettNy(new Fagsak(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, AktørId.dummy(), AktørId.dummy(), null, new Saksnummer("1337"), LocalDate.now(), LocalDate.now()));
        fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        behandling = Behandling.forFørstegangssøknad(fagsak)
            .medBehandlingStatus(BehandlingStatus.UTREDES)
            .medOpprettetDato(LocalDateTime.now())
            .medBehandlingstidFrist(LocalDate.now().plusDays(30))
            .build();
        entityManager.persist(behandling);
        entityManager.flush();

        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medInnsendingstidspunkt(LocalDateTime.now())
            .medMottattDato(LocalDate.now())
            .medMottattTidspunkt(LocalDateTime.now())
            .medFagsakId(fagsakId)
            .medJournalPostId(new JournalpostId("2222233333"))
            .medType(Brevkode.INNTEKTSMELDING)
            .build();
        mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(mottattDokument);

        when(dokumentValidatorProvider.finnValidator(Brevkode.INNTEKTSMELDING)).thenReturn(dokumentValidator);
    }

    @Test
    void skalFeileNårDetFinnesÅpenProsesstaskPåBehandlingen() {
        var åpenTask = ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        åpenTask.setBehandling(fagsak.getId(), behandling.getId());
        fagsakProsessTaskRepository.lagreNyGruppe(åpenTask);

        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterMottattDokumentTask.class);
        prosessTaskData.setFagsakId(fagsak.getId());
        var håndterMottattDokumentTask = new HåndterMottattDokumentTask(behandlingRepositoryProvider, innhentDokumentTjeneste, mottatteDokumentTjeneste, dokumentValidatorProvider);

        var e = assertThrows(TekniskException.class, () -> håndterMottattDokumentTask.prosesser(prosessTaskData));
        assertThat(e.getMessage()).isEqualTo("K9-653311:Behandling [" + behandling.getId() + "] pågår, avventer å håndtere mottatt dokument til det er prosessert");
    }
}
