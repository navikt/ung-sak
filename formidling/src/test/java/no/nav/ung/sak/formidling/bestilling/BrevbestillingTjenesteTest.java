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
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingStatusType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlientFake;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.mottaker.PdlPerson;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.test.util.aktør.FiktiveFnr;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevbestillingTjenesteTest {


    @Inject
    private EntityManager entityManager;

    private DokArkivKlientFake dokArkivKlient;
    private BrevbestillingRepository brevbestillingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private BrevbestillingTjeneste brevbestillingTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    private String fnr = new FiktiveFnr().nesteFnr();

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        prosessTaskTjeneste = new ProsessTaskTjenesteImpl(new ProsessTaskRepositoryImpl(entityManager, null, null));
        dokArkivKlient = new DokArkivKlientFake();
        brevbestillingRepository = new BrevbestillingRepository(entityManager);
        brevbestillingTjeneste = new BrevbestillingTjeneste(brevbestillingRepository, dokArkivKlient, prosessTaskTjeneste);
    }

    @Test
    void skalLagreBestillingJournalføreOgLageDistribusjonstask_vedtaksbrev() {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        scenarioBuilder.lagre(repositoryProvider);
        var behandling = scenarioBuilder.getBehandling();
        behandling.avsluttBehandling();

        var dokument = "et dokument";

        PdlPerson testBruker = new PdlPerson(fnr, behandling.getAktørId(), "Test Bruker", null);
        var generertBrev = new GenerertBrev(
            new PdfGenDokument(dokument.getBytes(StandardCharsets.UTF_8), dokument),
            testBruker,
            testBruker,
            DokumentMalType.INNVILGELSE_DOK,
            TemplateType.INNVILGELSE
        );

        brevbestillingTjeneste.bestillBrev(behandling, generertBrev);


        var bestilling = brevbestillingRepository.hentForBehandling(behandling.getId()).getFirst();
        assertBrevbestilling(bestilling, behandling);

        assertThat(dokArkivKlient.getRequests()).hasSize(1);
        var request = dokArkivKlient.getRequests().getFirst();
        assertDokArkivRequest(request, bestilling.getBrevbestillingUuid(), behandling);
        assertThat(bestilling.getJournalpostId()).isEqualTo(dokArkivKlient.getResponses().getFirst().journalpostId());

        List<ProsessTaskData> distTasker = prosessTaskTjeneste.finnAlle(BrevdistribusjonTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(distTasker).hasSize(1);
        var disttask = distTasker.getFirst();
        assertThat(disttask.getPropertyValue(BrevdistribusjonTask.BREVBESTILLING_ID_PARAM)).isEqualTo(bestilling.getId().toString());
        assertThat(disttask.getPropertyValue(BrevdistribusjonTask.BREVBESTILLING_DISTRIBUSJONSTYPE)).isEqualTo(DistribuerJournalpostRequest.DistribusjonsType.VEDTAK.name());

    }

    @Test
    void skalLagreBestillingJournalføreOgLageDistribusjonstask_informasjonsbrev() {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        scenarioBuilder.lagre(repositoryProvider);
        var behandling = scenarioBuilder.getBehandling();

        var dokument = "et dokument";

        PdlPerson testBruker = new PdlPerson(fnr, behandling.getAktørId(), "Test Bruker", null);
        var generertBrev = new GenerertBrev(
            new PdfGenDokument(dokument.getBytes(StandardCharsets.UTF_8), dokument),
            testBruker,
            testBruker,
            DokumentMalType.GENERELT_FRITEKSTBREV,
            TemplateType.GENERELT_FRITEKSTBREV
        );

        brevbestillingTjeneste.bestillBrev(behandling, generertBrev);


        var bestilling = brevbestillingRepository.hentForBehandling(behandling.getId()).getFirst();
        assertThat(bestilling.getDokumentMalType()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV);
        assertThat(bestilling.getTemplateType()).isEqualTo(TemplateType.GENERELT_FRITEKSTBREV);
        assertThat(bestilling.isVedtaksbrev()).isFalse();

        assertThat(dokArkivKlient.getRequests()).hasSize(1);
        var request = dokArkivKlient.getRequests().getFirst();
        UUID dokumentBestillingId = bestilling.getBrevbestillingUuid();
        var tittel = "Ungdomsprogramytelse Fritekst generelt brev";
        assertThat(request.behandlingstema()).isNull();
        assertThat(request.tittel()).isEqualTo(tittel);

        // Verify Dokumenter
        assertThat(request.dokumenter()).hasSize(1);
        var dokument1 = request.dokumenter().getFirst();
        assertThat(dokument1.tittel()).isEqualTo(tittel);
        assertThat(dokument1.brevkode()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV.getKode());

        List<ProsessTaskData> distTasker = prosessTaskTjeneste.finnAlle(BrevdistribusjonTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(distTasker).hasSize(1);
        var disttask = distTasker.getFirst();
        assertThat(disttask.getPropertyValue(BrevdistribusjonTask.BREVBESTILLING_ID_PARAM)).isEqualTo(bestilling.getId().toString());
        assertThat(disttask.getPropertyValue(BrevdistribusjonTask.BREVBESTILLING_DISTRIBUSJONSTYPE)).isEqualTo(DistribuerJournalpostRequest.DistribusjonsType.VIKTIG.name());

    }

    private static void assertBrevbestilling(BrevbestillingEntitet bestilling, Behandling behandling) {
        assertThat(bestilling.getBrevbestillingUuid()).isNotNull();
        assertThat(bestilling.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(bestilling.getFagsakId()).isEqualTo(behandling.getFagsakId());
        assertThat(bestilling.getDokumentMalType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);
        assertThat(bestilling.getTemplateType()).isEqualTo(TemplateType.INNVILGELSE);
        assertThat(bestilling.getStatus()).isEqualTo(BrevbestillingStatusType.JOURNALFØRT);
        assertThat(bestilling.getDokdistBestillingId()).isNull();
        assertThat(bestilling.getMottaker().getMottakerId()).isEqualTo(behandling.getAktørId().getAktørId());
        assertThat(bestilling.getMottaker().getMottakerIdType()).isEqualTo(IdType.AKTØRID);
        assertThat(bestilling.isVedtaksbrev()).isTrue();
    }


    private void assertDokArkivRequest(OpprettJournalpostRequest request, UUID dokumentBestillingId, Behandling behandling) {
        var innvilgelseTittel = "Ungdomsprogramytelse Innvilgelse";
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
}
