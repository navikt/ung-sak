package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveSendTilInfotrygdTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class HenleggBehandlingTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private SøknadRepository søknadRepository;
    private FagsakRepository fagsakRepository;
    private HistorikkRepository historikkRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    public HenleggBehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                                     FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                     ProsessTaskRepository prosessTaskRepository) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
    }

    public void henleggBehandlingAvSaksbehandler(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        BehandlingskontrollKontekst kontekst =  behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknad.isEmpty()) {
            // Må ta behandling av vent for å tillate henleggelse (krav i Behandlingskontroll)
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        } else {
            // har søknad og saksbehandler forsøker å ta av vent
        }
        
        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, HistorikkAktør.SAKSBEHANDLER);
    }

    private void doHenleggBehandling(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse, HistorikkAktør historikkAktør) {
        BehandlingskontrollKontekst kontekst =  behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        fagsakProsessTaskRepository.ryddProsessTasks(behandling.getFagsakId(), behandling.getId());  // rydd tidligere tasks (eks. registerinnhenting, etc)
        behandlingskontrollTjeneste.henleggBehandling(kontekst, årsakKode);

        if (BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET.equals(årsakKode)) {
            sendHenleggelsesbrev(behandling.getId(), HistorikkAktør.VEDTAKSLØSNINGEN);
        } else if (BehandlingResultatType.MANGLER_BEREGNINGSREGLER.equals(årsakKode)) {
            fagsakRepository.fagsakSkalBehandlesAvInfotrygd(behandling.getFagsakId());
            opprettOppgaveTilInfotrygd(behandling);
        }
        lagHistorikkinnslagForHenleggelse(behandling.getId(), årsakKode, begrunnelse, historikkAktør);
    }

    /** Henlegger helt - for forvaltning først og fremst. */
    public void henleggBehandlingOgAksjonspunkter(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        BehandlingskontrollKontekst kontekst =  behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if(behandling.isBehandlingPåVent()) {
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
