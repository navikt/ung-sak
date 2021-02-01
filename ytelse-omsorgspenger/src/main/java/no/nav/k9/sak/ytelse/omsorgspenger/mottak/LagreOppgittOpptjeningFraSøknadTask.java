package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.søknad.Søknad;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * lagrer inntektsmeldinger til abakus asynk i egen task.
 */
@ApplicationScoped
@ProsessTask(LagreOppgittOpptjeningFraSøknadTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class LagreOppgittOpptjeningFraSøknadTask extends UnderBehandlingProsessTask {

    private static final Logger log = LoggerFactory.getLogger(LagreOppgittOpptjeningFraSøknadTask.class);

    static final String TASKTYPE = "lagre.oppgitt.opptjening.søknad.oms.til.abakus";

    private final SøknadParser søknadParser = new SøknadParser();
    private LagreOppgittOpptjening lagreOppgittOpptjeningTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;

    LagreOppgittOpptjeningFraSøknadTask() {
        // for proxy
    }

    @Inject
    public LagreOppgittOpptjeningFraSøknadTask(BehandlingRepository behandlingRepository,
                                               BehandlingLåsRepository behandlingLåsRepository,
                                               LagreOppgittOpptjening lagreOppgittOpptjeningTjeneste,
                                               MottatteDokumentRepository mottatteDokumentRepository) {
        super(behandlingRepository, behandlingLåsRepository);
        this.lagreOppgittOpptjeningTjeneste = lagreOppgittOpptjeningTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    protected void doProsesser(ProsessTaskData input, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();

        //henter alle som er til BEHANDLER
        List<MottattDokument> ubehandledeDokumenter = mottatteDokumentRepository.hentMottatteDokumentForBehandling(fagsakId, behandlingId, Brevkode.SØKNAD_UTBETALING_OMS, true, DokumentStatus.BEHANDLER);
        if (ubehandledeDokumenter.isEmpty()) {
            log.info("Fant ingen ubehandlede søknader om utbetaling av omsorgspenger nå - er allerede håndtert. Avbryter task");
            return;
        }

        mottatteDokumentRepository.oppdaterStatus(ubehandledeDokumenter, DokumentStatus.GYLDIG);

        Collection<Søknad> søknader = søknadParser.parseSøknader(ubehandledeDokumenter);
        if (søknader.size() != 1) {
            //TODO skal egentlig støtte dette
            throw new UnsupportedOperationException("Støtter ikke å behandle mer enn 1 søknad om gangen, fikk: " + søknader.size());
        }
        lagreOppgittOpptjeningTjeneste.lagreOpptjening(behandling, ZonedDateTime.now(), søknader.iterator().next().getYtelse());
    }

}
