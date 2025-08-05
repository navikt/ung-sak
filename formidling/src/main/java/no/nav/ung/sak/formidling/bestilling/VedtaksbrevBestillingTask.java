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
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevBestillingInput;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

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
    public static final String DOKUMENT_MAL_TYPE_PARAM = "dokumentMalType";

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevBestillingTask.class);

    private BehandlingRepository behandlingRepository;
    private VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste;
    private BrevbestillingRepository brevbestillingRepository;
    private BrevbestillingTjeneste brevbestillingTjeneste;

    @Inject
    public VedtaksbrevBestillingTask(
        BehandlingRepository behandlingRepository,
        VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste,
        BrevbestillingRepository brevbestillingRepository,
        BrevbestillingTjeneste brevbestillingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vedtaksbrevGenerererTjeneste = vedtaksbrevGenerererTjeneste;
        this.brevbestillingRepository = brevbestillingRepository;
        this.brevbestillingTjeneste = brevbestillingTjeneste;
    }

    VedtaksbrevBestillingTask() {
    }


    @Override
    protected void prosesser(ProsessTaskData prosessTaskData)  {
        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        DokumentMalType dokumentMalType = DokumentMalType.fraKode(prosessTaskData.getPropertyValue(DOKUMENT_MAL_TYPE_PARAM));
        validerBrevbestillingForespørsel(behandling, dokumentMalType);

        var generertBrev = vedtaksbrevGenerererTjeneste.genererVedtaksbrevForBehandling(new VedtaksbrevBestillingInput(behandling.getId(), false));
        if (generertBrev == null) {
            LOG.info("Ingen brev generert.");
            return;
        }

       brevbestillingTjeneste.bestillBrev(behandling, generertBrev);

    }

    private void validerBrevbestillingForespørsel(Behandling behandling, DokumentMalType dokumentMalType) {
        if (!behandling.erAvsluttet()) {
            throw new IllegalStateException("Behandling må være avsluttet for å kunne bestille vedtaksbrev");
        }

        var tidligereBestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        var tidligereVedtaksbrev= tidligereBestillinger.stream()
            .filter(BrevbestillingEntitet::isVedtaksbrev)
            .filter(it -> it.getDokumentMalType() == dokumentMalType)
            .toList();
        if (!tidligereVedtaksbrev.isEmpty()) {
            String collect = tidligereVedtaksbrev.stream()
                    .map(BrevbestillingEntitet::toString)
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Det finnes allerede en bestilling for samme vedtaksbrev: " + collect);
        }
    }

}
