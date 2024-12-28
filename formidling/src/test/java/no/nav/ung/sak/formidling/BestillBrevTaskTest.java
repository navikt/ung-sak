package no.nav.ung.sak.formidling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.CommonTaskProperties;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlient;
import no.nav.ung.sak.formidling.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.DokDistKlient;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BestillBrevTaskTest {

    @Inject
    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private BrevGenerererTjeneste brevGenerererTjeneste;
    private DokArkivKlient dokArkivKlient;
    private DokDistKlient dokDistKlient;

    private final String fnr = PdlKlientFake.gyldigFnr();

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        var pdlKlient = new PdlKlientFake("Test", "Testesen", fnr);
        brevGenerererTjeneste = new BrevGenerererTjeneste(
            repositoryProvider.getBehandlingRepository(),
            new PersonBasisTjeneste(pdlKlient),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient()
        );

        dokArkivKlient = new DokArkivKlient();
        dokDistKlient = new DokDistKlient();
    }

    @Test
    void skalLagePdfJournalføreOgDistribuere() {
        var ungdom = AktørId.dummy();
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad(ungdom);
        var behandling = scenarioBuilder.lagre(repositoryProvider);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        var dokumentBestillingId = UUID.randomUUID(); //TODO endre til id fra DB

        BestillBrevTask bestillBrevTask = new BestillBrevTask(brevGenerererTjeneste, dokArkivKlient, dokDistKlient);
        bestillBrevTask.doTask(lagTask(behandling));

        var request = dokArkivKlient.getRequests().getFirst();
        assertDokArkivRequest(request, dokumentBestillingId, behandling);



    }

    private void assertDokArkivRequest(OpprettJournalpostRequest request, UUID dokumentBestillingId, Behandling behandling) {
        var innvilgelseTittel = "Ungdomsytelse Innvilgelse";
        // Verify main request fields
        assertThat(request.journalpostType()).isEqualTo("UTGAAENDE");
        assertThat(request.tema()).isEqualTo("OMS");
        assertThat(request.behandlingstema()).isEqualTo("ab0271");
        assertThat(request.tittel()).isEqualTo(innvilgelseTittel);
        assertThat(request.kanal()).isNull();
        assertThat(request.journalfoerendeEnhet()).isEqualTo("9999");
//        assertThat(request.eksternReferanseId()).isEqualTo(dokumentBestillingId.toString());

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
        assertThat(sak.fagsaksystem()).isEqualTo("K9");

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
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(BestillBrevTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
        prosessTaskData.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        prosessTaskData.setProperty(CommonTaskProperties.BEHANDLING_UUID, behandling.getUuid().toString());
        return prosessTaskData;
    }

}
