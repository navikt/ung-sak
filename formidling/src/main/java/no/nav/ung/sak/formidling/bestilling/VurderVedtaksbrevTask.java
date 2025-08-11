package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.*;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.formidling.vedtak.regler.*;
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
    private VedtaksbrevResultatRepository vedtaksbrevResultatRepository;
    private BehandlingRepository behandlingRepository;
    private BrevbestillingRepository brevbestillingRepository;

    VurderVedtaksbrevTask() {
        // for proxy
    }

    @Inject
    public VurderVedtaksbrevTask(
        VedtaksbrevRegler vedtaksbrevRegler,
        @KonfigVerdi(value = "IGNORE_MANGLENDE_BREV", defaultVerdi = "false") boolean ignoreManglendeBrev,
        ProsessTaskTjeneste prosessTaskTjeneste,
        VedtaksbrevValgRepository vedtaksbrevValgRepository,
        VedtaksbrevResultatRepository vedtaksbrevResultatRepository,
        BehandlingRepository behandlingRepository, BrevbestillingRepository brevbestillingRepository) {

        this.enableIgnoreManglendeBrev = ignoreManglendeBrev;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.vedtaksbrevResultatRepository = vedtaksbrevResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.brevbestillingRepository = brevbestillingRepository;
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

        resultat.vedtaksbrevResultater().forEach(it -> bestill(behandling, it));

    }

    private void validerBrevbestillingForespørsel(Behandling behandling, DokumentMalType dokumentMalType) {
        if (!behandling.erAvsluttet()) {
            throw new IllegalStateException("Behandling må være avsluttet for å kunne bestille vedtaksbrev");
        }

        var tidligereBestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        var tidligereVedtaksbrev = tidligereBestillinger.stream()
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

    public void bestill(Behandling behandling, Vedtaksbrev vedtaksbrev) {
        validerBrevbestillingForespørsel(behandling, vedtaksbrev.dokumentMalType());

        var bestilling = BrevbestillingEntitet.nyBrevbestilling(
            behandling.getFagsakId(),
            behandling.getId(),
            vedtaksbrev.dokumentMalType()
        );
        brevbestillingRepository.lagre(bestilling);

        var vedtaksbrevResultatEntitet = VedtaksbrevResultatEntitet.medBestilling(bestilling, vedtaksbrev.forklaring(), VedtaksbrevResultatType.BESTILT);
        vedtaksbrevResultatRepository.lagre(vedtaksbrevResultatEntitet);

        prosessTaskTjeneste.lagre(lagBestillingTask(behandling, bestilling.getId()));


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
            var bestilling = BrevbestillingEntitet.nyBrevbestilling(
                behandling.getFagsakId(),
                behandling.getId(),
                DokumentMalType.MANUELT_VEDTAK_DOK);
            brevbestillingRepository.lagre(bestilling);
            vedtaksbrevResultatRepository.lagre(VedtaksbrevResultatEntitet.medBestilling(bestilling, "Redigert vedtaksbrev", VedtaksbrevResultatType.BESTILT));
            prosessTaskTjeneste.lagre(lagBestillingTask(behandling, bestilling.getId()));

        }

    }

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
                vedtaksbrevResultatRepository.lagre(VedtaksbrevResultatEntitet
                    .utenBestilling(behandlingId, fagsakId, VedtaksbrevResultatType.IKKE_RELEVANT, forklaring));
            } else {
                throw new IllegalStateException("Feiler pga ingen brev implementert for tilfelle: " + forklaring);
            }
        }
        LOG.info("Ingen brev relevant for tilfelle: {}", forklaring);
        vedtaksbrevResultatRepository.lagre(VedtaksbrevResultatEntitet
            .utenBestilling(behandlingId, fagsakId, VedtaksbrevResultatType.IKKE_RELEVANT, forklaring));
    }

}
