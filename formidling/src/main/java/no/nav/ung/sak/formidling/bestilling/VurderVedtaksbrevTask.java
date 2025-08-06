package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.VedtaksbrevResultatEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.VedtaksbrevResultatRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.VedtaksbrevResultatType;
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
    private BrevbestillingTjeneste brevbestillingTjeneste;
    private VedtaksbrevResultatRepository vedtaksbrevResultatRepository;
    private BehandlingRepository behandlingRepository;

    VurderVedtaksbrevTask() {
        // for proxy
    }

    @Inject
    public VurderVedtaksbrevTask(
        VedtaksbrevRegler vedtaksbrevRegler,
        @KonfigVerdi(value = "IGNORE_MANGLENDE_BREV", defaultVerdi = "false") boolean ignoreManglendeBrev,
        ProsessTaskTjeneste prosessTaskTjeneste,
        VedtaksbrevValgRepository vedtaksbrevValgRepository,
        BrevbestillingTjeneste brevbestillingTjeneste,
        VedtaksbrevResultatRepository vedtaksbrevResultatRepository, BehandlingRepository behandlingRepository) {

        this.enableIgnoreManglendeBrev = ignoreManglendeBrev;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.brevbestillingTjeneste = brevbestillingTjeneste;
        this.vedtaksbrevResultatRepository = vedtaksbrevResultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var resultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("Resultat fra vedtaksbrev regler: {}", resultat.safePrint());

        if (!resultat.harBrev()) {
            håndterIngenBrevResultat(resultat, behandling);
            return;
        }

        var vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId).orElse(null);
        boolean harSaksbehandlerHindretEllerRedigertBrev = vedtaksbrevValgEntitet != null && (vedtaksbrevValgEntitet.isHindret() || vedtaksbrevValgEntitet.isRedigert());
        if (harSaksbehandlerHindretEllerRedigertBrev) {
            håndterSaksbehandlerValg(behandling, vedtaksbrevValgEntitet, resultat);
            return;
        }

        if (vedtaksbrevValgEntitet != null) {
            LOG.warn("Vedtaksbrevvalg lagret, men verken hindret eller redigert");
        }

        var bestillinger = resultat.vedtaksbrevResultater()
            .stream()
            .map(it -> lagreVedtaksbrevResultatOgOpprettBestilling(behandling, it))
            .toList();


        var tasker = bestillinger.stream()
            .map(it -> lagBestillingTask(behandling, it.getId()))
            .toList();


        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(tasker);
        prosessTaskTjeneste.lagre(gruppe);
    }

    private BrevbestillingEntitet lagreVedtaksbrevResultatOgOpprettBestilling(Behandling behandling, Vedtaksbrev vedtaksbrev) {
        Long fagsakId = behandling.getFagsakId();
        Long behandlingId = behandling.getId();

        var bestilling = brevbestillingTjeneste.nyBestilling(behandling, vedtaksbrev.dokumentMalType());
        vedtaksbrevResultatRepository.lagre(VedtaksbrevResultatEntitet.medBestilling(behandlingId, fagsakId, VedtaksbrevResultatType.BESTILT, vedtaksbrev.forklaring(), bestilling));
        return bestilling;
    }

    private void håndterSaksbehandlerValg(Behandling behandling, VedtaksbrevValgEntitet vedtaksbrevValg, BehandlingVedtaksbrevResultat resultat) {
        Long behandlingId = behandling.getId();
        Long fagsakId = behandling.getFagsakId();

        if (vedtaksbrevValg.isHindret()) {
            LOG.info("Vedtaksbrev er manuelt stoppet - bestiller ikke brev");

            vedtaksbrevResultatRepository.lagre(VedtaksbrevResultatEntitet.utenBestilling(behandlingId, fagsakId, VedtaksbrevResultatType.HINDRET_SAKSBEHANDLER, null));
            return;
        }

        if (vedtaksbrevValg.isRedigert()) {
            LOG.info("Vedtaksbrev er manuelt redigert - bestiller manuell brev");
            if (resultat.vedtaksbrevResultater().stream().noneMatch(it -> it.vedtaksbrevEgenskaper().kanRedigere())) {
                throw new IllegalStateException("Redigering ikke tilatt, men er redigert. " + vedtaksbrevValg);
            }
            //TODO håndtere flere vedtaksbrev
            var brevbestillingEntitet = brevbestillingTjeneste.nyBestilling(behandling, DokumentMalType.MANUELT_VEDTAK_DOK);
            vedtaksbrevResultatRepository.lagre(VedtaksbrevResultatEntitet.medBestilling(behandlingId, fagsakId, VedtaksbrevResultatType.BESTILT, "Redigert automatisk vedtaksbrev", brevbestillingEntitet));
            prosessTaskTjeneste.lagre(lagBestillingTask(behandling, brevbestillingEntitet.getId()));

        }

    }

    @NotNull
    private static ProsessTaskData lagBestillingTask(Behandling behandling, Long brevbestillingId) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(VedtaksbrevBestillingTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId());
        prosessTaskData.setProperty(VedtaksbrevBestillingTask.BREVBESTILLING_ID, brevbestillingId.toString());
        return prosessTaskData;
    }


    private void håndterIngenBrevResultat(BehandlingVedtaksbrevResultat resultat, Behandling behandling) {
        String forklaring = resultat.ingenBrevResultater().stream().map(VedtaksbrevRegelResultat::forklaring).collect(Collectors.joining(", ", "[", "]"));
        var behandlingId = behandling.getId();
        var fagsakId = behandling.getFagsakId();
        var ikkeImplementerteBrev = resultat.ingenBrevResultater().stream()
            .filter(it -> it.ingenBrevÅrsakType() == IngenBrevÅrsakType.IKKE_IMPLEMENTERT)
            .toList();

        if (!ikkeImplementerteBrev.isEmpty()) {
            if (enableIgnoreManglendeBrev) {
                LOG.warn("Ingen brev implementert for tilfelle pga: {}", forklaring);
                vedtaksbrevResultatRepository.lagre(VedtaksbrevResultatEntitet.utenBestilling(behandlingId, fagsakId, VedtaksbrevResultatType.IKKE_RELEVANT, forklaring));
            } else {
                throw new IllegalStateException("Feiler pga ingen brev implementert for tilfelle: " + forklaring);
            }
        }
        LOG.info("Ingen brev relevant for tilfelle: {}", forklaring);
        vedtaksbrevResultatRepository.lagre(VedtaksbrevResultatEntitet.utenBestilling(behandlingId, fagsakId, VedtaksbrevResultatType.IKKE_RELEVANT, forklaring));
    }

}
