package no.nav.ung.sak.domene.behandling.steg.iverksettevedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.kontrakt.dokument.BestillBrevDtoGammel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class HenleggBehandlingTjeneste {

    private static final Logger log = LoggerFactory.getLogger(HenleggBehandlingTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ProsessTaskTjeneste prosessTaskRepository;
    private SøknadRepository søknadRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Instance<HenleggelsePostopsTjeneste> henleggelsePostopsTjenester;

    public HenleggBehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                     ProsessTaskTjeneste prosessTaskRepository,
                                     MottatteDokumentRepository mottatteDokumentRepository,
                                     @Any Instance<HenleggelsePostopsTjeneste> henleggelsePostopsTjenester) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.henleggelsePostopsTjenester = henleggelsePostopsTjenester;
    }

    public void henleggBehandlingAvSaksbehandler(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        if (Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN, FagsakYtelseType.OPPLÆRINGSPENGER).contains(behandling.getFagsakYtelseType())) {
            throw new IllegalArgumentException("Det er p.t. ikke støttet å henlegge behandlinger for fagsak " + behandling.getFagsakYtelseType().getNavn());
        }

        validerÅrsakMotKrav(årsakKode, behandling);

        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknad.isEmpty()) {
            // Må ta behandling av vent for å tillate henleggelse (krav i Behandlingskontroll)
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        } else {
            // har søknad og saksbehandler forsøker å ta av vent
        }

        fagsakProsessTaskRepository.ryddProsessTasks(behandling.getFagsakId(), behandling.getId());  // rydd tidligere tasks (eks. registerinnhenting, etc)

        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, HistorikkAktør.SAKSBEHANDLER);
    }

    private void validerÅrsakMotKrav(BehandlingResultatType årsakKode, Behandling behandling) {
        if (Objects.equals(behandling.getFagsakYtelseType(), FagsakYtelseType.OMP) && Objects.equals(BehandlingResultatType.HENLAGT_FEILOPPRETTET, årsakKode)) {
            var gyldigeDokumenterPåBehandling = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
                .stream()
                .filter(it -> Objects.equals(it.getBehandlingId(), behandling.getId()))
                .toList();

            if (!gyldigeDokumenterPåBehandling.isEmpty()) {
                throw new IllegalStateException("Prøver å henlegge behandling som feilopprettet men det finnes gyldige/ubehandlede dokumenter på sak");
            }
        }
    }

    private void doHenleggBehandling(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse, HistorikkAktør historikkAktør) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        behandlingskontrollTjeneste.henleggBehandling(kontekst, årsakKode);

        henleggDokumenter(behandling);

        if (BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET.equals(årsakKode)) {
            sendHenleggelsesbrev(behandling.getId(), HistorikkAktør.VEDTAKSLØSNINGEN);
        }
        lagHistorikkinnslagForHenleggelse(behandling, årsakKode, begrunnelse, historikkAktør);

        HenleggelsePostopsTjeneste.finnTjeneste(henleggelsePostopsTjenester, behandling.getFagsakYtelseType())
            .ifPresent(postOps -> postOps.utfør(behandling));
    }

    private void henleggDokumenter(Behandling behandling) {

        Set<Brevkode> kravdokumentTyper = new HashSet<>(Brevkode.SØKNAD_TYPER);

        List<MottattDokument> gyldigeDokumenterFagsak = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId());
        List<MottattDokument> gyldigeKravdokumenterBehandling = gyldigeDokumenterFagsak.stream()
            .filter(dok -> behandling.getId().equals(dok.getBehandlingId()))
            .filter(dok -> kravdokumentTyper.contains(dok.getType()))
            .toList();

        log.info("Henlegger behandling og {} dokumenter", gyldigeDokumenterFagsak.size());
        for (MottattDokument kravdokument : gyldigeKravdokumenterBehandling) {
            log.info("Henlegger kravdokument med journalpostId {} og type {}", kravdokument.getJournalpostId().getVerdi(), kravdokument.getType().getKode());
            mottatteDokumentRepository.lagre(kravdokument, DokumentStatus.HENLAGT);
        }
    }

    /**
     * Henlegger helt - for forvaltning først og fremst.
     */
    public void henleggBehandlingOgAksjonspunkter(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        fagsakProsessTaskRepository.ryddProsessTasks(behandling.getFagsakId(), behandling.getId());  // rydd tidligere tasks (eks. registerinnhenting, etc)

        if (behandling.isBehandlingHenlagt()) {
            // er allerede henlagt - en saksbehandler kom oss i forkjøpet
            Fagsak fagsak = behandling.getFagsak();
            log.warn("Behandling [fagsakId={}, saksnummer={}, behandlingId={}, ytelseType={}] er allerede henlagt", fagsak.getId(), fagsak.getSaksnummer(), behandling.getId(), fagsak.getYtelseType());
            return;
        }

        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        }

        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    private void sendHenleggelsesbrev(long behandlingId, HistorikkAktør aktør) {
        BestillBrevDtoGammel bestillBrevDto = new BestillBrevDtoGammel(behandlingId, DokumentMalType.HENLEGG_BEHANDLING_DOK);
        // TODO: send brev
//        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, aktør);
    }

    private void lagHistorikkinnslagForHenleggelse(Behandling behandling, BehandlingResultatType aarsak, String begrunnelse, HistorikkAktør aktør) {
        var historikkinnslag = opprettHistorikkinnslagForHenleggelse(behandling, aarsak, begrunnelse, HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    public static Historikkinnslag opprettHistorikkinnslagForHenleggelse(Behandling behandling, BehandlingResultatType årsakKode, String begrunnelse, HistorikkAktør aktør) {
        return new Historikkinnslag.Builder()
            .medAktør(aktør)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medTittel("Behandling er henlagt")
            .addLinje(årsakKode.getNavn())
            .addLinje(begrunnelse)
            .build();
    }
}
