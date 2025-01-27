package no.nav.ung.sak.formidling.bestilling;


import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.formidling.dokdist.DokDistRestKlient;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest.DistribusjonsType;

/**
 * Distribuerer en bestilling via dokumentdistribusjon
 */
@ApplicationScoped
@ProsessTask(value = BrevdistribusjonTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class BrevdistribusjonTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "formidling.brevdistribusjon";

    private static final Logger LOG = LoggerFactory.getLogger(BrevdistribusjonTask.class);

    static final String BREVBESTILLING_ID_PARAM = "brevbestillingId";
    static final String BREVBESTILLING_DISTRIBUSJONSTYPE = "brevbestilling.distribusjonstype";
    private BrevbestillingRepository brevbestillingRepository;
    private DokDistRestKlient dokDistRestKlient;

    @Inject
    public BrevdistribusjonTask(BrevbestillingRepository brevbestillingRepository, DokDistRestKlient dokDistRestKlient) {
        this.brevbestillingRepository = brevbestillingRepository;
        this.dokDistRestKlient = dokDistRestKlient;
    }

    public BrevdistribusjonTask() {
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        //TODO set logcontext
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

        var response = dokDistRestKlient.distribuer(new DistribuerJournalpostRequest(
            bestilling.getJournalpostId(),
            Fagsystem.K9SAK.getOffisiellKode(),
            "UNG_SAK",
            DistribusjonsType.valueOf(distribusjonstype),
            "KJERNETID"

        ));
        bestilling.fullført(response.bestillingsId());
        brevbestillingRepository.lagre(bestilling);

        LOG.info("Brevbestilling OK {}", bestilling);
    }
}
