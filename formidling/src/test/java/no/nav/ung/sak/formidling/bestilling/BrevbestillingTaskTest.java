package no.nav.ung.sak.formidling.bestilling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BehandlingBrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingStatusType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.BrevGenerererTjeneste;
import no.nav.ung.sak.formidling.BrevGenerererTjenesteFake;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlientFake;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest.DistribusjonsType;
import no.nav.ung.sak.test.util.aktør.FiktiveFnr;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevbestillingTaskTest {

    @Inject
    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;
    private BrevGenerererTjeneste brevGenerererTjeneste;
    private DokArkivKlientFake dokArkivKlient;
    private BrevbestillingRepository brevbestillingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private String fnr;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        prosessTaskTjeneste = new ProsessTaskTjenesteImpl(new ProsessTaskRepositoryImpl(entityManager, null, null));
        fnr = new FiktiveFnr().nesteFnr();
        brevGenerererTjeneste = new BrevGenerererTjenesteFake(fnr);

        dokArkivKlient = new DokArkivKlientFake();

        brevbestillingRepository = new BrevbestillingRepository(entityManager);

    }

    @Test
    void skalLagreBestillingLagePdfJournalføreOgLageDistribusjonstask() {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        scenarioBuilder.lagre(repositoryProvider);
        var behandling = scenarioBuilder.getBehandling();
        behandling.avsluttBehandling();

        BrevbestillingTask brevBestillingTask = new BrevbestillingTask(behandlingRepository, brevGenerererTjeneste, brevbestillingRepository, dokArkivKlient, prosessTaskTjeneste);
        brevBestillingTask.doTask(lagTask(behandling));

        BehandlingBrevbestillingEntitet behandlingBestilling = brevbestillingRepository.hentForBehandling(behandling.getId()).getFirst();
        assertThat(behandlingBestilling.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(behandlingBestilling.isVedtaksbrev()).isTrue();

        var bestilling = behandlingBestilling.getBestilling();
        assertBrevbestilling(bestilling, behandling);

        assertThat(dokArkivKlient.getRequests()).hasSize(1);
        var request = dokArkivKlient.getRequests().getFirst();
        assertDokArkivRequest(request, bestilling.getBrevbestillingUuid(), behandling);
        assertThat(bestilling.getJournalpostId()).isEqualTo(dokArkivKlient.getResponses().getFirst().journalpostId());

        List<ProsessTaskData> distTasker = prosessTaskTjeneste.finnAlle(BrevdistribusjonTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(distTasker).hasSize(1);
        var disttask = distTasker.getFirst();
        assertThat(disttask.getPropertyValue(BrevdistribusjonTask.BREVBESTILLING_ID_PARAM)).isEqualTo(bestilling.getId().toString());
        assertThat(disttask.getPropertyValue(BrevdistribusjonTask.BREVBESTILLING_DISTRIBUSJONSTYPE)).isEqualTo(DistribusjonsType.VEDTAK.name());

    }

    private static void assertBrevbestilling(BrevbestillingEntitet bestilling, Behandling behandling) {
        assertThat(bestilling.getBrevbestillingUuid()).isNotNull();
        assertThat(bestilling.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(bestilling.getDokumentMalType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);
        assertThat(bestilling.getTemplateType()).isEqualTo(TemplateType.INNVILGELSE);
        assertThat(bestilling.getStatus()).isEqualTo(BrevbestillingStatusType.JOURNALFØRT);
        assertThat(bestilling.getDokumentData()).isNull();
        assertThat(bestilling.getDokdistBestillingId()).isNull();
        assertThat(bestilling.getMottaker().getMottakerId()).isEqualTo(behandling.getAktørId().getAktørId());
        assertThat(bestilling.getMottaker().getMottakerIdType()).isEqualTo(IdType.AKTØRID);
    }




    @Test
    void skalIkkeLagreBestillingHvisJournalføringFeiler() {
        //TODO
    }

    @Test
    void skalFeileHvisBehandlingIFeilTilstand() {
        //TODO
    }

    @Test
    void skalTillateMaksEttVedtaksbrevForBehandling() {
        //TODO
    }


    private void assertDokArkivRequest(OpprettJournalpostRequest request, UUID dokumentBestillingId, Behandling behandling) {
        var innvilgelseTittel = "Ungdomsytelse Innvilgelse";
        // Verify main request fields
        assertThat(request.journalpostType()).isEqualTo("UTGAAENDE");
        assertThat(request.tema()).isEqualTo("UNG");
        assertThat(request.behandlingstema()).isNull();
        assertThat(request.tittel()).isEqualTo(innvilgelseTittel);
        assertThat(request.kanal()).isNull();
        assertThat(request.journalfoerendeEnhet()).isEqualTo("9999");
        assertThat(request.eksternReferanseId()).isEqualTo(dokumentBestillingId.toString());

        // Verify AvsenderMottaker
        var avsenderMottaker = request.avsenderMottaker();
        assertThat(avsenderMottaker.id()).isEqualTo(fnr);
        assertThat(avsenderMottaker.navn()).isNull();
        assertThat(avsenderMottaker.land()).isNull();
        assertThat(avsenderMottaker.idType()).isEqualTo(OpprettJournalpostRequest.AvsenderMottaker.IdType.FNR);

        // Verify Bruker
        var bruker = request.bruker();
        assertThat(bruker.id()).isEqualTo(fnr);
        assertThat(bruker.idType()).isEqualTo(OpprettJournalpostRequest.Bruker.BrukerIdType.FNR);

        // Verify Tilleggsopplysninger
        assertThat(request.tilleggsopplysninger()).hasSize(1);
        var tilleggsopplysning = request.tilleggsopplysninger().getFirst();
        assertThat(tilleggsopplysning.nokkel()).isEqualTo("ung.formidling.eRef");
        assertThat(tilleggsopplysning.verdi()).isEqualTo(behandling.getUuid().toString());

        // Verify Sak
        var sak = request.sak();
        assertThat(sak.sakstype()).isEqualTo("FAGSAK");
        assertThat(sak.fagsakId()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(sak.fagsaksystem()).isEqualTo("UNG_SAK");

        // Verify Dokumenter
        assertThat(request.dokumenter()).hasSize(1);
        var dokument = request.dokumenter().getFirst();
        assertThat(dokument.tittel()).isEqualTo(innvilgelseTittel);
        assertThat(dokument.brevkode()).isEqualTo(DokumentMalType.INNVILGELSE_DOK.getKode());
        assertThat(dokument.dokumentKategori()).isNull();

        assertThat(dokument.dokumentvarianter()).hasSize(1);
        var variant = dokument.dokumentvarianter().getFirst();
        assertThat(variant.variantformat()).isEqualTo("ARKIV");
        assertThat(variant.fysiskDokument()).isNotNull();
        assertThat(variant.filtype()).isEqualTo("PDFA");
    }

    private static ProsessTaskData lagTask(Behandling behandling) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(BrevbestillingTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId());
        return prosessTaskData;
    }

}
