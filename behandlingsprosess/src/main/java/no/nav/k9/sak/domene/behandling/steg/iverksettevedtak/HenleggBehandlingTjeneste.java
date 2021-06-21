package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
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
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
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

    public HenleggBehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                                     FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                     ProsessTaskRepository prosessTaskRepository,
                                     MottatteDokumentRepository mottatteDokumentRepository) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    public void henleggBehandlingAvSaksbehandler(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse, Map<Long, DokumentStatus> dokumenterMedNyStatus) {
        BehandlingskontrollKontekst kontekst =  behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknad.isEmpty()) {
            // Må ta behandling av vent for å tillate henleggelse (krav i Behandlingskontroll)
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        } else {
            // har søknad og saksbehandler forsøker å ta av vent
        }

        fagsakProsessTaskRepository.ryddProsessTasks(behandling.getFagsakId(), behandling.getId());  // rydd tidligere tasks (eks. registerinnhenting, etc)

        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, HistorikkAktør.SAKSBEHANDLER, dokumenterMedNyStatus);
    }

    private void doHenleggBehandling(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse, HistorikkAktør historikkAktør, Map<Long, DokumentStatus> dokumenterMedNyStatus) {
        BehandlingskontrollKontekst kontekst =  behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        behandlingskontrollTjeneste.henleggBehandling(kontekst, årsakKode);

        dokumenterMedNyStatus.forEach((dokId, nyStatus) -> {
            var dokument = mottatteDokumentRepository.hentMottattDokument(dokId).orElseThrow();
            validerDokument(dokument, behandling);
            mottatteDokumentRepository.lagre(dokument, nyStatus);
        });

        if (BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET.equals(årsakKode)) {
            sendHenleggelsesbrev(behandling.getId(), HistorikkAktør.VEDTAKSLØSNINGEN);
        } else if (BehandlingResultatType.MANGLER_BEREGNINGSREGLER.equals(årsakKode)) {
            fagsakRepository.fagsakSkalBehandlesAvInfotrygd(behandling.getFagsakId());
            opprettOppgaveTilInfotrygd(behandling);
        }
        lagHistorikkinnslagForHenleggelse(behandling.getId(), årsakKode, begrunnelse, historikkAktør);
    }

    private void validerDokument(MottattDokument dokument, Behandling behandling) {
        if (!dokument.getBehandlingId().equals(behandling.getId())) {
            throw new IllegalArgumentException("Kan ikke oppdatere status på behandling som ikke tilhører behandling");
        }
    }

    /** Henlegger helt - for forvaltning først og fremst. */
    public void henleggBehandlingOgAksjonspunkter(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        BehandlingskontrollKontekst kontekst =  behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        fagsakProsessTaskRepository.ryddProsessTasks(behandling.getFagsakId(), behandling.getId());  // rydd tidligere tasks (eks. registerinnhenting, etc)

        if(behandling.isBehandlingHenlagt()) {
            // er allerede henlagt - en saksbehandler kom oss i forkjøpet
            Fagsak fagsak = behandling.getFagsak();
            log.warn("Behandling [fagsakId={}, saksnummer={}, behandlingId={}, ytelseType={}] er allerede henlagt", fagsak.getId(), fagsak.getSaksnummer(), behandling.getId(), fagsak.getYtelseType());
            return;
        }

        if(behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        }

        // Henlegger ikke dokumenter fra denne forvaltningsmetoden - må gjøres som separat forvaltning-handling
        Map<Long, DokumentStatus> dokumenterMedNyStatus = Map.of();
        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, HistorikkAktør.VEDTAKSLØSNINGEN, dokumenterMedNyStatus);
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
