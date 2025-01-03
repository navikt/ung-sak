package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.BrevdistribusjonTask.BREVBESTILLING_DISTRIBUSJONSTYPE;
import static no.nav.ung.sak.formidling.BrevdistribusjonTask.BREVBESTILLING_ID_PARAM;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.dokdist.DokDistRestKlientFake;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest.DistribusjonsType;
import no.nav.ung.sak.formidling.domene.BrevMottaker;
import no.nav.ung.sak.formidling.domene.BrevbestillingEntitet;
import no.nav.ung.sak.formidling.domene.BrevbestillingStatusType;
import no.nav.ung.sak.formidling.template.TemplateType;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevdistribusjonTaskTest {
    @Inject
    private EntityManager entityManager;

    private DokDistRestKlientFake dokDistKlient;
    private BrevbestillingRepository brevbestillingRepository;

    @BeforeEach
    void setUp() {
        dokDistKlient = new DokDistRestKlientFake();
        brevbestillingRepository = new BrevbestillingRepository(entityManager);
    }

    @Test
    void skalDistribuere() {
        var bestilling = BrevbestillingEntitet.nyBrevbestilling(
            "ABC",
            DokumentMalType.INNVILGELSE_DOK,
            new BrevMottaker("123", IdType.AKTØRID));

        String jp123 = "jp123";
        bestilling.generertOgJournalført(TemplateType.INNVILGELSE, jp123);
        brevbestillingRepository.lagre(bestilling);

        var pd = ProsessTaskData.forProsessTask(BrevdistribusjonTask.class);
        pd.setProperty(BREVBESTILLING_ID_PARAM, bestilling.getId().toString());
        pd.setProperty(BREVBESTILLING_DISTRIBUSJONSTYPE, DistribusjonsType.VEDTAK.name());

        var task = new BrevdistribusjonTask(brevbestillingRepository, dokDistKlient);
        task.doTask(pd);

        assertThat(dokDistKlient.getRequests()).hasSize(1);
        DistribuerJournalpostRequest req = dokDistKlient.getRequests().getFirst();
        assertThat(req.journalpostId()).isEqualTo(bestilling.getJournalpostId());
        assertThat(req.bestillendeFagsystem()).isEqualTo(Fagsystem.K9SAK.getOffisiellKode());
        assertThat(req.distribusjonstidspunkt()).isEqualTo("KJERNETID");
        assertThat(req.distribusjonstype()).isEqualTo(DistribusjonsType.VEDTAK);
        assertThat(req.dokumentProdApp()).isEqualTo("UNG_SAK");

        BrevbestillingEntitet oppdatertBestilling = brevbestillingRepository.hent(bestilling.getId());
        assertThat(oppdatertBestilling.getDokdistBestillingId()).isEqualTo(dokDistKlient.getResponses().getFirst().bestillingsId());
        assertThat(oppdatertBestilling.getStatus()).isEqualTo(BrevbestillingStatusType.FULLFØRT);

    }

    @Test
    void skalFeileHvisIkkeJournalførtStatus() {
        //TODO
    }
}
