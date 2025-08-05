package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.formidling.vedtak.regler.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@ApplicationScoped
@ProsessTask(value = VedtaksbrevBestillingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderVedtaksbrevTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(VurderVedtaksbrevTask.class);

    public static final String TASKTYPE = "formidling.vedtak.brevvurdering";

    private boolean enableIgnoreManglendeBrev;
    private VedtaksbrevRegler vedtaksbrevRegler;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;


    VurderVedtaksbrevTask() {
        // for proxy
    }

    @Inject
    public VurderVedtaksbrevTask(
        VedtaksbrevRegler vedtaksbrevRegler,
        @KonfigVerdi(value = "IGNORE_MANGLENDE_BREV", defaultVerdi = "false") boolean ignoreManglendeBrev, ProsessTaskTjeneste prosessTaskTjeneste, VedtaksbrevValgRepository vedtaksbrevValgRepository) {

        this.enableIgnoreManglendeBrev = ignoreManglendeBrev;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
    }


    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var resultat = vedtaksbrevRegler.kjør(behandlingId);
        if (!resultat.harBrev()) {
            håndterIngenBrevResultat(resultat);
            return;
        }


        var vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId).orElse(null);
//        if (vedtaksbrevValgEntitet != null) {
//            if (resultat.vedtaksbrevResultater().stream().filter(it -> it.vedtaksbrevEgenskaper() != null && it.vedtaksbrevEgenskaper().kanRedigere()).) {
//                throw IllegalStateException("")
//            }
//            if (vedtaksbrevValgEntitet.isHindret()) {
//                LOG.info("Vedtaksbrev er manuelt stoppet - lager ikke brev");
//                return null;
//            }
//            if (vedtaksbrevValgEntitet.isRedigert()) {
//                LOG.info("Vedtaksbrev er manuelt redigert - genererer manuell brev");
//                return doGenererManuellVedtaksbrev(vedtaksbrevBestillingInput);
//            }
//            LOG.warn("Vedtaksbrevvalg lagret, men verken hindret eller redigert");
//        }
//
//        return doGenererAutomatiskVedtaksbrev(vedtaksbrevBestillingInput);



        var tasker = resultat.vedtaksbrevResultater().stream()
            .filter(it -> it.dokumentMalType() != null)
            .map(it -> lagBestillingTask(it, prosessTaskData))
            .toList();


        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(tasker);
        prosessTaskTjeneste.lagre(gruppe);
    }

    @NotNull
    private static ProsessTaskData lagBestillingTask(Vedtaksbrev resultat, ProsessTaskData forrigeProsessTask) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(VedtaksbrevBestillingTask.class);
        prosessTaskData.setBehandling(forrigeProsessTask.getFagsakId(), Long.valueOf(forrigeProsessTask.getBehandlingId()));
        prosessTaskData.setProperty(VedtaksbrevBestillingTask.DOKUMENT_MAL_TYPE_PARAM, resultat.dokumentMalType().getKode());
        return prosessTaskData;
    }


    private void håndterIngenBrevResultat(BehandlingVedtaksbrevResultat resultat) {
        String forklaring = resultat.ingenBrevResultater().stream().map(VedtaksbrevRegelResultat::forklaring).collect(Collectors.joining(", ", "[", "]"));

        var ikkeImplementerteBrev = resultat.ingenBrevResultater().stream()
            .filter(it -> it.ingenBrevÅrsakType() == IngenBrevÅrsakType.IKKE_IMPLEMENTERT)
            .toList();

        if (!ikkeImplementerteBrev.isEmpty()) {
            if (enableIgnoreManglendeBrev) {
                LOG.warn("Ingen brev implementert for tilfelle pga: {}", forklaring);
            } else {
                throw new IllegalStateException("Feiler pga ingen brev implementert for tilfelle: " + forklaring);
            }
        }
        LOG.info("Ingen brev relevant for tilfelle: {}", forklaring);
    }
}
