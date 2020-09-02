package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.k9.sak.mottak.inntektsmelding.InntektsmeldingParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(HåndterMottattDokumentTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HåndterMottattDokumentTask extends FagsakProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.håndterMottattDokument";
    public static final String MOTTATT_DOKUMENT_ID_KEY = "mottattDokumentId";
    public static final String BEHANDLING_ÅRSAK_TYPE_KEY = "arsakType";

    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private final InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    HåndterMottattDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public HåndterMottattDokumentTask(BehandlingRepositoryProvider repositoryProvider,
                                      InnhentDokumentTjeneste innhentDokumentTjeneste,
                                      MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.innhentDokumentTjeneste = innhentDokumentTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        List<String> dokumentIder = Arrays.asList(prosessTaskData.getPropertyValue(HåndterMottattDokumentTask.MOTTATT_DOKUMENT_ID_KEY).split("[\\s,]+"));

        List<MottattDokument> mottatteDokumenter = finnDokumenter(prosessTaskData.getBehandlingId(), dokumentIder);
        var fagsak = fagsakRepository.finnEksaktFagsak(prosessTaskData.getFagsakId());
        innhentDokumentTjeneste.utfør(fagsak, mottatteDokumenter);
    }

    private List<MottattDokument> finnDokumenter(String behandlingId, List<String> dokumentIder) {
        List<MottattDokument> dokumenter = new ArrayList<>();

        for (var dokId : dokumentIder) {
            Long dokumentId = Long.parseLong(dokId);
            MottattDokument mottattDokument = mottatteDokumentTjeneste.hentMottattDokument(dokumentId)
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: HåndterMottattDokument uten gyldig mottatt dokument, id=" + dokumentId));

            if (behandlingId == null && mottattDokument.harPayload()) {
                inntektsmeldingParser.xmlTilWrapper(mottattDokument); // gjør en tidlig validering
            }
            dokumenter.add(mottattDokument);
        }
        return dokumenter;
    }

    @Override
    protected List<String> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(prosessTaskData.getFagsakId()).stream().map(String::valueOf).collect(Collectors.toList());
    }
}
