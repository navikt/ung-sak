package no.nav.k9.sak.mottak.midlertidig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(HåndterFortaptDokumentTask.TASKTYPE)
public class HåndterFortaptDokumentTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "forvaltning.håndterFortaptDokument";
    public static final String MOTTATT_DOKUMENT_ID_KEY = "mottattDokumentId";
    private static final Logger log = LoggerFactory.getLogger(HåndterFortaptDokumentTask.class);
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;


    HåndterFortaptDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public HåndterFortaptDokumentTask(BehandlingRepositoryProvider repositoryProvider,
                                      MottatteDokumentRepository mottatteDokumentRepository,
                                      MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dokumentId = Long.parseLong(prosessTaskData.getPropertyValue(HåndterFortaptDokumentTask.MOTTATT_DOKUMENT_ID_KEY));
        var feilmelding = "Utviklerfeil: HåndterMottattDokument uten gyldig mottatt dokument, id=" + dokumentId;
        var mottattDokument = mottatteDokumentRepository.hentMottattDokument(dokumentId)
            .orElseThrow(() -> new IllegalStateException(feilmelding));

        var behandling = behandlingRepository.hentBehandling(mottattDokument.getBehandlingId());
        log.info("Lagrer fortapt dokument='{}' på behandling='{}'", mottattDokument, behandling);

        mottatteDokumentTjeneste.persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(behandling, mottattDokument);
    }
}
