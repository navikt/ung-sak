package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(HåndterMottattDokumentTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HåndterMottattDokumentTask extends FagsakProsessTask {

    private static final Logger log = LoggerFactory.getLogger(HåndterMottattDokumentTask.class);

    public static final String TASKTYPE = "innhentsaksopplysninger.håndterMottattDokument";
    public static final String MOTTATT_DOKUMENT_ID_KEY = "mottattDokumentId";
    public static final String BEHANDLING_ÅRSAK_TYPE_KEY = "arsakType";

    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private DokumentValidatorProvider dokumentValidatorProvider;

    HåndterMottattDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public HåndterMottattDokumentTask(BehandlingRepositoryProvider repositoryProvider,
                                      InnhentDokumentTjeneste innhentDokumentTjeneste,
                                      MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                      DokumentValidatorProvider dokumentValidatorProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.innhentDokumentTjeneste = innhentDokumentTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.dokumentValidatorProvider = dokumentValidatorProvider;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        var behandlingId = prosessTaskData.getBehandlingId();

        // hent alle dokumenter markert mottatt
        List<MottattDokument> mottatteDokumenter = mottatteDokumentTjeneste.hentMottatteDokumentPåFagsak(fagsakId, true, DokumentStatus.MOTTATT)
            .stream()
            // gamle inntektsmeldinger kan ha status null, men vil være koblet til behandlingId (skal ikke ta på nytt her)
            .filter(m -> m.getBehandlingId() == null)
            .collect(Collectors.toList());

        if (mottatteDokumenter.isEmpty()) {
            log.info("Ingen dokumenter fortsatt markert MOTTATT, avbryter denne tasken (behandlet av tidligere kjøring)");
            return;
        }

        validerDokumenter(behandlingId, mottatteDokumenter);

        mottatteDokumentTjeneste.oppdaterStatus(mottatteDokumenter, DokumentStatus.BEHANDLER);

        var fagsak = fagsakRepository.finnEksaktFagsak(prosessTaskData.getFagsakId());
        innhentDokumentTjeneste.mottaDokument(fagsak, mottatteDokumenter);
    }

    @Override
    protected List<String> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(prosessTaskData.getFagsakId()).stream().map(String::valueOf).collect(Collectors.toList());
    }

    private void validerDokumenter(String behandlingId, Collection<MottattDokument> mottatteDokumenter) {
        DokumentValidator validator = dokumentValidatorProvider.finnValidator(mottatteDokumenter);
        validator.validerDokumenter(behandlingId, mottatteDokumenter);
    }

}
