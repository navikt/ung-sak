package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.formidling.vedtak.regler.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@ApplicationScoped
@ProsessTask(value = VurderVedtaksbrevTask.TASKTYPE)
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

        //TODO lagre resultat i databasen.  + lag flere tester + verdikjedetest.

        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var resultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("Resultat fra vedtaksbrev regler: {}", resultat.safePrint());

        if (!resultat.harBrev()) {
            håndterIngenBrevResultat(resultat);
            return;
        }

        var vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId).orElse(null);
        boolean harSaksbehandlerHindretEllerRedigertBrev = vedtaksbrevValgEntitet != null && (vedtaksbrevValgEntitet.isHindret() || vedtaksbrevValgEntitet.isRedigert());
        if (harSaksbehandlerHindretEllerRedigertBrev) {
            håndterSaksbehandlerValg(prosessTaskData, vedtaksbrevValgEntitet, resultat);
            return;
        }

        if (vedtaksbrevValgEntitet != null) {
            LOG.warn("Vedtaksbrevvalg lagret, men verken hindret eller redigert");
        }

        var tasker = resultat.vedtaksbrevResultater().stream()
            .map(it -> lagBestillingTask(it, prosessTaskData))
            .toList();


        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(tasker);
        prosessTaskTjeneste.lagre(gruppe);
    }

    private void håndterSaksbehandlerValg(ProsessTaskData prosessTaskData, VedtaksbrevValgEntitet vedtaksbrevValg, BehandlingVedtaksbrevResultat resultat) {
        if (vedtaksbrevValg.isHindret()) {
            LOG.info("Vedtaksbrev er manuelt stoppet - bestiller ikke brev");
            return;
        }

        if (vedtaksbrevValg.isRedigert()) {
            LOG.info("Vedtaksbrev er manuelt redigert - bestiller manuell brev");
            if (resultat.vedtaksbrevResultater().stream().noneMatch(it -> it.vedtaksbrevEgenskaper().kanRedigere())) {
                throw new IllegalStateException("Redigering ikke tilatt, men er redigert. " + vedtaksbrevValg);
            }
            //TODO håndtere flere vedtaksbrev
            prosessTaskTjeneste.lagre(lagManuellBrevBestillingTask(prosessTaskData));

        }

    }

    @NotNull
    private static ProsessTaskData lagBestillingTask(Vedtaksbrev resultat, ProsessTaskData forrigeProsessTask) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(VedtaksbrevBestillingTask.class);
        prosessTaskData.setBehandling(forrigeProsessTask.getFagsakId(), Long.valueOf(forrigeProsessTask.getBehandlingId()));
        prosessTaskData.setProperty(VedtaksbrevBestillingTask.DOKUMENT_MAL_TYPE_PARAM, resultat.dokumentMalType().getKode());
        return prosessTaskData;
    }

    @NotNull
    private static ProsessTaskData lagManuellBrevBestillingTask(ProsessTaskData forrigeProsessTask) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(VedtaksbrevBestillingTask.class);
        prosessTaskData.setBehandling(forrigeProsessTask.getFagsakId(), Long.valueOf(forrigeProsessTask.getBehandlingId()));
        prosessTaskData.setProperty(VedtaksbrevBestillingTask.MANUELL_BREV_PARAM, "true");
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
