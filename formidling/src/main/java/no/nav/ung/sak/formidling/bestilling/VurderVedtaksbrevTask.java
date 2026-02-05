package no.nav.ung.sak.formidling.bestilling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
import no.nav.ung.sak.formidling.innhold.TomVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegel;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegelResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@ApplicationScoped
@ProsessTask(value = VurderVedtaksbrevTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderVedtaksbrevTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(VurderVedtaksbrevTask.class);

    public static final String TASKTYPE = "formidling.vedtak.brevvurdering";

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;
    private BehandlingVedtaksbrevRepository behandlingVedtaksbrevRepository;
    private BehandlingRepository behandlingRepository;
    private BrevbestillingRepository brevbestillingRepository;
    private Instance<VedtaksbrevRegel> vedtaksbrevRegler;

    VurderVedtaksbrevTask() {
        // for proxy
    }

    @Inject
    public VurderVedtaksbrevTask(
        ProsessTaskTjeneste prosessTaskTjeneste,
        VedtaksbrevValgRepository vedtaksbrevValgRepository,
        BehandlingVedtaksbrevRepository behandlingVedtaksbrevRepository,
        BehandlingRepository behandlingRepository,
        BrevbestillingRepository brevbestillingRepository,
        @Any Instance<VedtaksbrevRegel> vedtaksbrevRegler) {

        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.behandlingVedtaksbrevRepository = behandlingVedtaksbrevRepository;
        this.behandlingRepository = behandlingRepository;
        this.brevbestillingRepository = brevbestillingRepository;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var vedtaksbrevRegler = VedtaksbrevRegel.hentVedtaksbrevRegel(this.vedtaksbrevRegler, behandling.getFagsakYtelseType(), behandling.getType());
        var resultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("Resultat fra vedtaksbrev regler: {}", resultat.safePrint());

        if (!resultat.harBrev()) {
            håndterIngenBrevResultat(resultat, behandling);
            return;
        }

        var hindredeEllerRedigerteValg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId).stream()
            .filter(it -> it.isHindret() || it.isRedigert())
            .toList();

        hindredeEllerRedigerteValg.forEach(it -> håndterSaksbehandlerValg(behandling, it, resultat));

        var hindredeEllerRedigerteMaler = hindredeEllerRedigerteValg.stream()
            .map(VedtaksbrevValgEntitet::getDokumentMalType)
            .collect(Collectors.toSet());

        var vedtaksbrevResultater = resultat.vedtaksbrevResultater().stream()
            .filter(it -> !hindredeEllerRedigerteMaler.contains(it.dokumentMalType()))
            .toList();

        for (int brevNr = 0; brevNr < vedtaksbrevResultater.size(); brevNr++) {
            Vedtaksbrev it = vedtaksbrevResultater.get(brevNr);
            bestill(behandling, it, brevNr);
        }

    }

    private void validerBrevbestillingForespørsel(Behandling behandling, Vedtaksbrev vedtaksbrev) {
        if (!behandling.erAvsluttet() &&
            !DokumentMalType.KLAGE_OVERSENDT_KLAGEINSTANS.equals(vedtaksbrev.dokumentMalType())) { // Unntak for oversendt til klageinstans, som ikke avslutter behandling før den er ferdig behandlet i kabal
            throw new IllegalStateException("Behandling må være avsluttet for å kunne bestille vedtaksbrev");
        }

        if (vedtaksbrev.vedtaksbrevBygger().getClass().equals(TomVedtaksbrevInnholdBygger.class)) {
            throw new IllegalStateException("Kan ikke bestille vedtaksbrev der bygger er TomVedtaksbrevInnholdBygger");
        }

        var tidligereBestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        var tidligereVedtaksbrev = tidligereBestillinger.stream()
            .filter(BrevbestillingEntitet::isVedtaksbrev)
            .filter(it -> it.getDokumentMalType() == vedtaksbrev.dokumentMalType())
            .toList();
        if (!tidligereVedtaksbrev.isEmpty()) {
            String collect = tidligereVedtaksbrev.stream()
                .map(BrevbestillingEntitet::toString)
                .collect(Collectors.joining(", "));
            throw new IllegalStateException("Det finnes allerede en bestilling for samme vedtaksbrev: " + collect);
        }
    }

    public void bestill(Behandling behandling, Vedtaksbrev vedtaksbrev, int brevNr) {
        validerBrevbestillingForespørsel(behandling, vedtaksbrev);

        var bestilling = BrevbestillingEntitet.nyBrevbestilling(
            behandling.getFagsakId(),
            behandling.getId(),
            vedtaksbrev.dokumentMalType()
        );
        brevbestillingRepository.lagre(bestilling);

        var vedtaksbrevResultatEntitet = BehandlingVedtaksbrev
            .medBestilling(bestilling, vedtaksbrev.forklaring(), VedtaksbrevResultatType.BESTILT, null);
        behandlingVedtaksbrevRepository.lagre(vedtaksbrevResultatEntitet);

        prosessTaskTjeneste.lagre(lagBestillingTask(behandling, bestilling.getId(), brevNr));
        LOG.info("Opprettet vedtaksbrev bestilling med id: {} og mal {}", bestilling.getId(), bestilling.getDokumentMalType().getKode());

    }


    private void håndterSaksbehandlerValg(Behandling behandling, VedtaksbrevValgEntitet vedtaksbrevValg, BehandlingVedtaksbrevResultat resultat) {
        Long behandlingId = behandling.getId();
        Long fagsakId = behandling.getFagsakId();

        var vedtaksbrev = resultat.finnVedtaksbrev(vedtaksbrevValg.getDokumentMalType())
            .orElseThrow(() -> new IllegalStateException("Har valg for vedtaksbrev mal " + vedtaksbrevValg.getDokumentMalType() + ", men malen er ikke tillatt for behandlingen. "));

        if (vedtaksbrevValg.isHindret()) {
            LOG.info("Vedtaksbrev er manuelt stoppet - bestiller ikke brev");

            behandlingVedtaksbrevRepository.lagre(
                BehandlingVedtaksbrev.utenBestilling(behandlingId, fagsakId, VedtaksbrevResultatType.HINDRET_SAKSBEHANDLER, null, vedtaksbrevValg));
            return;
        }

        if (vedtaksbrevValg.isRedigert()) {
            LOG.info("Vedtaksbrev er manuelt redigert - bestiller manuell brev");
            if (!vedtaksbrev.vedtaksbrevEgenskaper().kanRedigere()) {
                throw new IllegalStateException("Redigering ikke tilatt, men er redigert. " + vedtaksbrevValg);
            }
            var bestilling = BrevbestillingEntitet.nyBrevbestilling(
                behandling.getFagsakId(),
                behandling.getId(),
                DokumentMalType.MANUELT_VEDTAK_DOK);
            brevbestillingRepository.lagre(bestilling);

            behandlingVedtaksbrevRepository.lagre(BehandlingVedtaksbrev
                .medBestilling(bestilling, "Redigert vedtaksbrev", VedtaksbrevResultatType.BESTILT, vedtaksbrevValg));

            ProsessTaskData prosessTaskData = lagBestillingTask(behandling, bestilling.getId(), 0);
            prosessTaskData.setProperty(VedtaksbrevBestillingTask.VEDTAKSBREV_VALG_ID, vedtaksbrevValg.getId().toString());
            prosessTaskTjeneste.lagre(prosessTaskData);

        }

    }

    private static ProsessTaskData lagBestillingTask(Behandling behandling, Long brevbestillingId, int brevNr) {
        ProsessTaskData prosessTaskData = BrevbestillingTaskGenerator
            .formidlingProsessTaskIGruppe(VedtaksbrevBestillingTask.class, behandling.getFagsakId(), brevNr);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId());
        prosessTaskData.setSaksnummer(behandling.getFagsak().getSaksnummer().toString());
        prosessTaskData.setProperty(VedtaksbrevBestillingTask.BREVBESTILLING_ID, brevbestillingId.toString());
        prosessTaskData.setCallIdFraEksisterende();
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
            throw new IllegalStateException("Feiler pga ingen brev implementert for tilfelle: " + forklaring);
        }
        LOG.info("Ingen brev relevant for tilfelle: {}", forklaring);
        behandlingVedtaksbrevRepository.lagre(BehandlingVedtaksbrev
            .utenBestilling(behandlingId, fagsakId, VedtaksbrevResultatType.IKKE_RELEVANT, forklaring, null));
    }

}
