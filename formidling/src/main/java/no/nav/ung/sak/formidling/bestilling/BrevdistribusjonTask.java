package no.nav.ung.sak.formidling.bestilling;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingStatusType;
import no.nav.ung.sak.formidling.BrevGenereringSemafor;
import no.nav.ung.sak.formidling.BrevHistorikkinnslagTjeneste;
import no.nav.ung.sak.formidling.dokdist.DokDistRestKlient;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest;
import no.nav.ung.sak.formidling.dokdist.dto.DistribuerJournalpostRequest.DistribusjonsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Distribuerer en bestilling via dokumentdistribusjon
 */
@ApplicationScoped
@ProsessTask(value = BrevdistribusjonTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class BrevdistribusjonTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "formidling.brevdistribusjon";

    private static final Logger LOG = LoggerFactory.getLogger(BrevdistribusjonTask.class);

    static final String BREVBESTILLING_ID_PARAM = "brevbestillingId";
    static final String BREVBESTILLING_DISTRIBUSJONSTYPE = "brevbestilling.distribusjonstype";
    private BrevbestillingRepository brevbestillingRepository;
    private DokDistRestKlient dokDistRestKlient;
    private BrevHistorikkinnslagTjeneste brevHistorikkinnslagTjeneste;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public BrevdistribusjonTask(BrevbestillingRepository brevbestillingRepository, DokDistRestKlient dokDistRestKlient, BrevHistorikkinnslagTjeneste brevHistorikkinnslagTjeneste, ProsessTaskTjeneste taskTjeneste) {
        this.brevbestillingRepository = brevbestillingRepository;
        this.dokDistRestKlient = dokDistRestKlient;
        this.brevHistorikkinnslagTjeneste = brevHistorikkinnslagTjeneste;
        this.taskTjeneste = taskTjeneste;
    }

    public BrevdistribusjonTask() {
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        try {
            distribuerBrev(prosessTaskData);
        } catch (BrevGenereringSemafor.BrevGenereringSemaforIkkeTilgjengeligException e){
            String propertyName = "retriesPgaSemaforUtilgjengelig";
            int antallTidligereRekjøringer = Integer.parseInt(prosessTaskData.getProperties().getProperty(propertyName, "0"));

            if (antallTidligereRekjøringer < 10){
                //reschedulerer task istedet for at den skal vente og ta opp en tråd som kan brukes for å kjøre andre tasker
                String rekjøringer = String.valueOf(antallTidligereRekjøringer + 1);
                prosessTaskData.setProperty(propertyName, rekjøringer);
                LOG.info("Fikk ikke semafor for brevgenerering, oppretter ny task til å kjøre senere, forsøk {}", rekjøringer);
                prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(10));
                taskTjeneste.lagre(prosessTaskData);
            } else {
                //måtte vente lenger enn forventet uten å få til å sende brev, kaster exception så det kan følges opp i overvåkning og eventuelt feilsøkes
                throw e;
            }
        }
    }

    private void distribuerBrev(ProsessTaskData prosessTaskData) {
        var bestillingId = Objects.requireNonNull(
            prosessTaskData.getPropertyValue(BREVBESTILLING_ID_PARAM), "Mangler brevbestilling");

        var distribusjonstype = Objects.requireNonNull(
            prosessTaskData.getPropertyValue(BREVBESTILLING_DISTRIBUSJONSTYPE), "Mangler distribusjonstype");

        BrevbestillingEntitet bestilling = brevbestillingRepository.hent(Long.valueOf(bestillingId));

        valider(bestilling);

        var response = dokDistRestKlient.distribuer(new DistribuerJournalpostRequest(
            bestilling.getJournalpostId(),
            Fagsystem.UNG_SAK.getOffisiellKode(),
            "UNG_SAK",
            DistribusjonsType.valueOf(distribusjonstype),
            "KJERNETID"

        ));
        bestilling.fullført(response.bestillingsId());
        brevbestillingRepository.lagre(bestilling);

        brevHistorikkinnslagTjeneste.opprett(bestilling);

        LOG.info("Brevbestilling OK {}", bestilling);
    }

    private static void valider(BrevbestillingEntitet bestilling) {
        if (bestilling.getStatus() != BrevbestillingStatusType.JOURNALFØRT) {
            throw new IllegalStateException("Krever at bestillingen har status JOURNALFØRT. Brevbestilling: " + bestilling);
        }

        Objects.requireNonNull(bestilling.getJournalpostId(), "Krever at bestillingen har journalpostId. Brevbestilling: " + bestilling);
    }
}
