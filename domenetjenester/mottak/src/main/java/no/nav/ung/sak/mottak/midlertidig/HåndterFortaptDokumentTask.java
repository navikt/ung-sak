package no.nav.ung.sak.mottak.midlertidig;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;

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

        mottatteDokumentTjeneste.persisterInntektsmeldingForBehandling(behandling, List.of(mottattDokument));
    }
}
