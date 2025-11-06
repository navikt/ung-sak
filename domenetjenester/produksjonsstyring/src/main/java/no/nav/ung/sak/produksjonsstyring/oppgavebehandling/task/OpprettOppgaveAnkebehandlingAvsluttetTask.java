package no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE;


@ApplicationScoped
@ProsessTask(OpprettOppgaveAnkebehandlingAvsluttetTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveAnkebehandlingAvsluttetTask extends BehandlingProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveAnkeAvsluttet";
    public static final String UTFALL = "utfall";
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveAnkebehandlingAvsluttetTask.class);
    private OppgaveTjeneste oppgaveTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private boolean historikkinnslagVedAvsluttetAnke;

    OpprettOppgaveAnkebehandlingAvsluttetTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveAnkebehandlingAvsluttetTask(
        OppgaveTjeneste oppgaveTjeneste,
        BehandlingRepositoryProvider repositoryProvider,
        HistorikkinnslagRepository historikkinnslagRepository,
        @KonfigVerdi(value = "HISTORIKKINNSLAG_AVSLUTTET_ANKE", required = false, defaultVerdi = "false") boolean historikkinnslagVedAvsluttetAnke) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.historikkinnslagVedAvsluttetAnke = historikkinnslagVedAvsluttetAnke;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        String oppgaveId = oppgaveTjeneste.opprettOppgaveOmAnke(prosessTaskData.getBehandlingId(), VURDER_KONSEKVENS_YTELSE, prosessTaskData.getPropertyValue(UTFALL));
        if (oppgaveId != null) {
            log.info("Oppgave i Gosys opprettet for avsluttet anke. Oppgavenummer: {}", oppgaveId);
            if (historikkinnslagVedAvsluttetAnke) {
                var begrunnelse = "Anke avsluttet med utfall " + prosessTaskData.getPropertyValue("utfall") + ", oppgave opprettet i Gosys.";
                lagHistorikkinnslagForAnkeAvsluttetGoys(Long.parseLong(prosessTaskData.getBehandlingId()), begrunnelse);
            } else {
                log.info("Historikkinnslag for avsluttet anke ble ikke opprettet fordi featuren ikke er på.");
            }
            log.info("Historikkinnslag opprettet for avsluttet anke. Oppgavenummer: {}", oppgaveId);
        } else {
            log.error("Oppgave i Gosys ble ikke opprettet for avsluttet anke!");
            if (historikkinnslagVedAvsluttetAnke) {
                var begrunnelse = "Anke avsluttet med utfall " + prosessTaskData.getPropertyValue("utfall") + ", oppgave er IKKE opprettet i Gosys.";
                lagHistorikkinnslagForAnkeAvsluttetGoys(Long.parseLong(prosessTaskData.getBehandlingId()), begrunnelse);
            }
        }
    }

    private void lagHistorikkinnslagForAnkeAvsluttetGoys(Long behandlingId, String begrunnelse) {
        Historikkinnslag.Builder builder = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medBehandlingId(behandlingId)
            .medTittel("Ankebehandling")
            .addLinje(begrunnelse);

        historikkinnslagRepository.lagre(builder.build());
    }
}
