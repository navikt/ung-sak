package no.nav.ung.sak.formidling;


import java.util.Objects;

import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.formidling.dokdist.DokDistKlient;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest.DistribusjonsType;
import no.nav.ung.sak.formidling.domene.BrevbestillingEntitet;
import no.nav.ung.sak.formidling.domene.BrevbestillingStatusType;

/**
 *
 */
//@ApplicationScoped
@ProsessTask(value = BrevdistribusjonTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class BrevdistribusjonTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "formidling.brevdistribusjon";

    static final String BREVBESTILLING_ID_PARAM = "brevbestillingId";
    static final String BREVBESTILLING_DISTRIBUSJONSTYPE = "brevbestilling.distribusjonstype";
    private BrevbestillingRepository brevbestillingRepository;
    private DokDistKlient dokDistKlient;

    @Inject
    public BrevdistribusjonTask(BrevbestillingRepository brevbestillingRepository, DokDistKlient dokDistKlient) {
        this.brevbestillingRepository = brevbestillingRepository;
        this.dokDistKlient = dokDistKlient;
    }

    public BrevdistribusjonTask() {
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var bestillingId = Objects.requireNonNull(
            prosessTaskData.getPropertyValue(BREVBESTILLING_ID_PARAM), "Mangler brevbestilling");

        var distribusjonstype = Objects.requireNonNull(
            prosessTaskData.getPropertyValue(BREVBESTILLING_DISTRIBUSJONSTYPE), "Mangler distribusjonstype");

        BrevbestillingEntitet bestilling = brevbestillingRepository.hent(Long.valueOf(bestillingId));

        if (bestilling.getStatus() != BrevbestillingStatusType.JOURNALFØRT) {
            throw new IllegalStateException("Krever at bestillingen har status JOURNALFØRT. Brevbestilling: " + bestilling);
        }

        if (bestilling.getJournalpostId() == null) {
            throw new IllegalStateException("Krever at bestillingen har journalpostId. Brevbestilling: " + bestilling);
        }

        var response = dokDistKlient.distribuer(new DistribuerJournalpostRequest(
            bestilling.getJournalpostId(),
            Fagsystem.K9SAK.getOffisiellKode(),
            "UNG_SAK",
            DistribusjonsType.valueOf(distribusjonstype),
            "KJERNETID"

        ));
        bestilling.fullført(response.bestillingsId());
        brevbestillingRepository.lagre(bestilling);

    }
}
