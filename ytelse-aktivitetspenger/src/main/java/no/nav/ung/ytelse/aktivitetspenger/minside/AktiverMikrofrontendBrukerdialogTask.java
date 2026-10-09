package no.nav.ung.ytelse.aktivitetspenger.minside;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.brukerdialog.kontrakt.sak.mikrofrontend.AktiverMikrofrontendRequest;
import no.nav.ung.brukerdialog.typer.AktørId;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.vedtak.brukerdialog.UngBrukerdialogSakKlient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ber ung-brukerdialog-api om å aktivere Min side-mikrofrontenden for aktivitetspenger for brukeren på fagsaken.
 */
@ApplicationScoped
@ProsessTask(AktiverMikrofrontendBrukerdialogTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AktiverMikrofrontendBrukerdialogTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "brukerdialog.aktiver.mikrofrontend";

    private static final Logger log = LoggerFactory.getLogger(AktiverMikrofrontendBrukerdialogTask.class);

    private FagsakRepository fagsakRepository;
    private UngBrukerdialogSakKlient klient;

    AktiverMikrofrontendBrukerdialogTask() {
        // for CDI proxy
    }

    @Inject
    public AktiverMikrofrontendBrukerdialogTask(FagsakRepository fagsakRepository,
                                                UngBrukerdialogSakKlient klient) {
        this.fagsakRepository = fagsakRepository;
        this.klient = klient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsak = fagsakRepository.finnEksaktFagsak(prosessTaskData.getFagsakId());

        var request = new AktiverMikrofrontendRequest(new AktørId(fagsak.getAktørId().getId()));
        klient.aktiverMikrofrontend(request);

        log.info("Ba ung-brukerdialog-api om å aktivere mikrofrontend for aktivitetspenger for saksnummer={}", fagsak.getSaksnummer());
    }
}
