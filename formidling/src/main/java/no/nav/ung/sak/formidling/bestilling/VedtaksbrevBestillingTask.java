package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingRepository;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererInput;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererTjeneste;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <a href="https://confluence.adeo.no/pages/viewpage.action?pageId=377701645">dokarkiv doc</a>
 * <p>
 * <a href="https://dokarkiv-q2.dev.intern.nav.no/swagger-ui/index.html#/">dokarkiv-q2.dev swagger</a>
 */
@ApplicationScoped
@ProsessTask(value = VedtaksbrevBestillingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VedtaksbrevBestillingTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "formidling.vedtak.brevbestilling";
    public static final String BREVBESTILLING_ID = "brevbestillingId";

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevBestillingTask.class);

    private BehandlingRepository behandlingRepository;
    private VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste;
    private JournalføringOgDistribusjonsTjeneste journalføringOgDistribusjonsTjeneste;
    private VedtaksbrevRegler vedtaksbrevRegler;
    private BrevbestillingRepository brevbestillingRepository;

    @Inject
    public VedtaksbrevBestillingTask(
        BehandlingRepository behandlingRepository,
        VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste,
        JournalføringOgDistribusjonsTjeneste journalføringOgDistribusjonsTjeneste,
        VedtaksbrevRegler vedtaksbrevRegler, BrevbestillingRepository brevbestillingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vedtaksbrevGenerererTjeneste = vedtaksbrevGenerererTjeneste;
        this.journalføringOgDistribusjonsTjeneste = journalføringOgDistribusjonsTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.brevbestillingRepository = brevbestillingRepository;
    }

    VedtaksbrevBestillingTask() {
    }


    @Override
    protected void prosesser(ProsessTaskData prosessTaskData)  {
        Objects.requireNonNull(prosessTaskData.getPropertyValue(BREVBESTILLING_ID), "Må ha brevbestillingId");

        var brevbestilling = brevbestillingRepository.hent(Long.valueOf(prosessTaskData.getPropertyValue(BREVBESTILLING_ID)));

        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        DokumentMalType dokumentMalType = brevbestilling.getDokumentMalType();

        if (dokumentMalType == DokumentMalType.MANUELT_VEDTAK_DOK) {
            GenerertBrev generertBrev = vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(behandling.getId(), dokumentMalType, false);
            journalføringOgDistribusjonsTjeneste.journalførOgDistribuerISekvens(behandling, brevbestilling, generertBrev);
            return;
        }

        genererOgJournalførAutomatiskBrev(behandling, brevbestilling);

    }

    private void genererOgJournalførAutomatiskBrev(Behandling behandling, BrevbestillingEntitet brevbestilling) {
        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());

        DokumentMalType dokumentMalType = brevbestilling.getDokumentMalType();
        Vedtaksbrev vedtaksbrev = totalresultater.finnVedtaksbrev(dokumentMalType)
            .orElseThrow(() -> new IllegalStateException("DokumentmalType " + dokumentMalType + " er ikke gyldig. Resultat fra regler: " + totalresultater.safePrint()));

        var generertBrev = vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(
            new VedtaksbrevGenerererInput(behandling.getId(), vedtaksbrev, totalresultater.detaljertResultatTimeline(), false));

        journalføringOgDistribusjonsTjeneste.journalførOgDistribuerISekvens(behandling, brevbestilling, generertBrev);
    }

}
