package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingRepository;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererTjeneste;
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
    public static final String VEDTAKSBREV_VALG_ID = "vedtaksbrevValgId";

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevBestillingTask.class);

    private BehandlingRepository behandlingRepository;
    private JournalføringOgDistribusjonsTjeneste journalføringOgDistribusjonsTjeneste;
    private BrevbestillingRepository brevbestillingRepository;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    @Inject
    public VedtaksbrevBestillingTask(
        BehandlingRepository behandlingRepository,
        JournalføringOgDistribusjonsTjeneste journalføringOgDistribusjonsTjeneste,
        BrevbestillingRepository brevbestillingRepository, VedtaksbrevValgRepository vedtaksbrevValgRepository) {
        this.behandlingRepository = behandlingRepository;
        this.journalføringOgDistribusjonsTjeneste = journalføringOgDistribusjonsTjeneste;
        this.brevbestillingRepository = brevbestillingRepository;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
    }

    VedtaksbrevBestillingTask() {
    }


    @Override
    protected void prosesser(ProsessTaskData prosessTaskData)  {
        Objects.requireNonNull(prosessTaskData.getPropertyValue(BREVBESTILLING_ID), "Må ha brevbestillingId");

        var brevbestilling = brevbestillingRepository.hent(Long.valueOf(prosessTaskData.getPropertyValue(BREVBESTILLING_ID)));

        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        DokumentMalType dokumentMalType = brevbestilling.getDokumentMalType();
        var vedtaksbrevGenerererTjeneste = hentVedtaksbrevGenererer(behandling);

        if (dokumentMalType == DokumentMalType.MANUELT_VEDTAK_DOK) {
            Long valgId = Long.valueOf(prosessTaskData.getPropertyValue(VEDTAKSBREV_VALG_ID));
            VedtaksbrevValgEntitet valg = vedtaksbrevValgRepository.hentVedtaksbrevValg(valgId);
            var generertBrev = vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(behandling.getId(), dokumentMalType, valg.getRedigertBrevHtml(), false);
            journalføringOgDistribusjonsTjeneste.journalførOgDistribuerISekvens(behandling, brevbestilling, generertBrev);
            return;
        }

        var generertBrev = vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(behandling, dokumentMalType, false);
        journalføringOgDistribusjonsTjeneste.journalførOgDistribuerISekvens(behandling, brevbestilling, generertBrev);
    }


    private VedtaksbrevGenerererTjeneste hentVedtaksbrevGenererer(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VedtaksbrevGenerererTjeneste.class, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VedtaksbrevGenerererTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }
}
