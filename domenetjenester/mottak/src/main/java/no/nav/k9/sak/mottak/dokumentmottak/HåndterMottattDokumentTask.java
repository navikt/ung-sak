package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;

@ApplicationScoped
@ProsessTask(value = HåndterMottattDokumentTask.TASKTYPE, maxFailedRuns = 10, firstDelay = 120, thenDelay = 120)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HåndterMottattDokumentTask extends FagsakProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.håndterMottattDokument";
    public static final String MOTTATT_DOKUMENT_ID_KEY = "mottattDokumentId";
    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");
    private static final Logger log = LoggerFactory.getLogger(HåndterMottattDokumentTask.class);
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
        var behandlingId = prosessTaskData.getBehandlingId() != null ? Long.valueOf(prosessTaskData.getBehandlingId()) : null;
        var fagsak = fagsakRepository.finnEksaktFagsak(prosessTaskData.getFagsakId());

        FagsakProsessTask.logContext(fagsak);

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
        LOG_CONTEXT.add("journalpostId", String.join(",", mottatteDokumenter.stream().map(d -> d.getJournalpostId().getVerdi()).toList()));
        for (MottattDokument mottattDokument : mottatteDokumenter) {
            log.info("Håndterer mottak av journalpostId {}", mottattDokument.getJournalpostId().getVerdi());
        }

        validerDokumenter(behandlingId, mottatteDokumenter);

        mottatteDokumentTjeneste.oppdaterStatus(mottatteDokumenter, DokumentStatus.BEHANDLER);

        innhentDokumentTjeneste.mottaDokument(fagsak, mottatteDokumenter);
    }

    @Override
    protected List<String> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(prosessTaskData.getFagsakId()).stream().map(String::valueOf).collect(Collectors.toList());
    }

    private void validerDokumenter(Long behandlingId, Collection<MottattDokument> mottatteDokumenter) {
        var mottatteDokumenterMap = mottatteDokumenter.stream()
            .collect(Collectors.groupingBy(MottattDokument::getType));

        mottatteDokumenterMap.keySet()
            .stream()
            .sorted(Brevkode.COMP_REKKEFØLGE)
            .forEach(key -> {
                DokumentValidator validator = dokumentValidatorProvider.finnValidator(key);
                validator.validerDokumenter(behandlingId, mottatteDokumenterMap.get(key));
            });
    }

}
