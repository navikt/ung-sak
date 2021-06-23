package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveSendTilInfotrygdTask;

@ApplicationScoped
public class HenleggBehandlingTjeneste {

    private static final Logger log = LoggerFactory.getLogger(HenleggBehandlingTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private SøknadRepository søknadRepository;
    private FagsakRepository fagsakRepository;
    private HistorikkRepository historikkRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Boolean henleggDokumenterLansert;

    public HenleggBehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                                     FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                     ProsessTaskRepository prosessTaskRepository,
                                     MottatteDokumentRepository mottatteDokumentRepository,
                                     @KonfigVerdi(value = "HENLEGG_DOKUMENTER", defaultVerdi = "true") Boolean henleggDokumenterLansert) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.henleggDokumenterLansert = henleggDokumenterLansert;
    }

    public void henleggBehandlingAvSaksbehandler(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        if (behandling.getFagsakYtelseType() == FagsakYtelseType.PSB) {
            throw new IllegalArgumentException("Det er p.t. ikke støttet å henlegge behandlinger for fagsak Pleiepenger sykt barn.");
        }

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

    private void doHenleggBehandling(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse, HistorikkAktør historikkAktør) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        behandlingskontrollTjeneste.henleggBehandling(kontekst, årsakKode);

        if (henleggDokumenterLansert) {
            henleggDokumenter(behandling);
        }

        if (BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET.equals(årsakKode)) {
            sendHenleggelsesbrev(behandling.getId(), HistorikkAktør.VEDTAKSLØSNINGEN);
        } else if (BehandlingResultatType.MANGLER_BEREGNINGSREGLER.equals(årsakKode)) {
            fagsakRepository.fagsakSkalBehandlesAvInfotrygd(behandling.getFagsakId());
            opprettOppgaveTilInfotrygd(behandling);
        }
        lagHistorikkinnslagForHenleggelse(behandling.getId(), årsakKode, begrunnelse, historikkAktør);
    }

    private void henleggDokumenter(Behandling behandling) {
        Set<Brevkode> kravdokumentTyper = new HashSet<>();
        kravdokumentTyper.addAll(Brevkode.SØKNAD_TYPER);
        kravdokumentTyper.add(Brevkode.INNTEKTSMELDING);

        List<MottattDokument> gyldigeDokumenterFagsak = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId());
        List<MottattDokument> gyldigeKravdokumenterBehandling = gyldigeDokumenterFagsak.stream()
            .filter(dok -> dok.getBehandlingId().equals(behandling.getId()))
            .filter(dok -> kravdokumentTyper.contains(dok.getType()))
            .collect(Collectors.toList());

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

    private void opprettOppgaveTilInfotrygd(Behandling behandling) {
        ProsessTaskData data = new ProsessTaskData(OpprettOppgaveSendTilInfotrygdTask.TASKTYPE);
        data.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        data.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(data);
    }

    private void sendHenleggelsesbrev(long behandlingId, HistorikkAktør aktør) {
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, DokumentMalType.HENLEGG_BEHANDLING_DOK);
        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, aktør);
    }

    private void lagHistorikkinnslagForHenleggelse(Long behandlingsId, BehandlingResultatType aarsak, String begrunnelse, HistorikkAktør aktør) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.AVBRUTT_BEH)
            .medÅrsak(aarsak)
            .medBegrunnelse(begrunnelse);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.AVBRUTT_BEH);
        historikkinnslag.setBehandlingId(behandlingsId);
        builder.build(historikkinnslag);

        historikkinnslag.setAktør(aktør);
        historikkRepository.lagre(historikkinnslag);
    }
}
