package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveSendTilInfotrygdTask;
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

    public HenleggBehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                                     ProsessTaskRepository prosessTaskRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
    }

    public void henleggBehandling(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, false);
    }

    private void doHenleggBehandling(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse, boolean avbrytVentendeAutopunkt) {
        BehandlingskontrollKontekst kontekst =  behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (avbrytVentendeAutopunkt && behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        } else {
            håndterHenleggelseUtenOppgitteSøknadsopplysninger(behandling, kontekst);
        }
        behandlingskontrollTjeneste.henleggBehandling(kontekst, årsakKode);

        if (BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET.equals(årsakKode)) {
            sendHenleggelsesbrev(behandling.getId(), HistorikkAktør.VEDTAKSLØSNINGEN);
        } else if (BehandlingResultatType.MANGLER_BEREGNINGSREGLER.equals(årsakKode)) {
            fagsakRepository.fagsakSkalBehandlesAvInfotrygd(behandling.getFagsakId());
            opprettOppgaveTilInfotrygd(behandling);
        }
        lagHistorikkinnslagForHenleggelse(behandling.getId(), årsakKode, begrunnelse, HistorikkAktør.SAKSBEHANDLER);
    }

    public void henleggBehandlingAvbrytAutopunkter(String behandlingId, BehandlingResultatType årsakKode, String begrunnelse) {
        doHenleggBehandling(behandlingId, årsakKode, begrunnelse, true);
    }

    private void opprettOppgaveTilInfotrygd(Behandling behandling) {
        ProsessTaskData data = new ProsessTaskData(OpprettOppgaveSendTilInfotrygdTask.TASKTYPE);
        data.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        data.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(data);
    }

    private void håndterHenleggelseUtenOppgitteSøknadsopplysninger(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        SøknadEntitet søknad = søknadRepository.hentSøknad(behandling);
        if (søknad == null) {
            // Må ta behandling av vent for å tillate henleggelse (krav i Behandlingskontroll)
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
        }
    }

    private void sendHenleggelsesbrev(long behandlingId, HistorikkAktør aktør) {
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, DokumentMalType.HENLEGG_BEHANDLING_DOK);
        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, aktør, false);
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
