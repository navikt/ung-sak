package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.k9.sak.mottak.inntektsmelding.InntektsmeldingPersistererTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(HåndterMottattDokumentTaskProperties.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class HåndterMottattDokumentTask extends FagsakProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.håndterMottattDokument";

    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BehandlingRepository behandlingRepository;
    private InntektsmeldingPersistererTjeneste dokumentPersistererTjeneste;

    HåndterMottattDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public HåndterMottattDokumentTask(BehandlingRepositoryProvider repositoryProvider, 
                                      InnhentDokumentTjeneste innhentDokumentTjeneste,
                                      InntektsmeldingPersistererTjeneste dokumentPersistererTjeneste, 
                                      MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.innhentDokumentTjeneste = innhentDokumentTjeneste;
        this.dokumentPersistererTjeneste = dokumentPersistererTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long dokumentId = Long.valueOf(prosessTaskData.getPropertyValue(HåndterMottattDokumentTaskProperties.MOTTATT_DOKUMENT_ID_KEY));
        String feilmelding = "Utviklerfeil: HåndterMottattDokument uten gyldig mottatt dokument, id=" + dokumentId;
        MottattDokument mottattDokument = mottatteDokumentTjeneste.hentMottattDokument(dokumentId)
                .orElseThrow(() -> new IllegalStateException(feilmelding));
        
        BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.UDEFINERT;
        if (prosessTaskData.getPropertyValue(HåndterMottattDokumentTaskProperties.BEHANDLING_ÅRSAK_TYPE_KEY) != null) {
            behandlingÅrsakType = BehandlingÅrsakType.fraKode(prosessTaskData.getPropertyValue(HåndterMottattDokumentTaskProperties.BEHANDLING_ÅRSAK_TYPE_KEY));
        } else if (prosessTaskData.getBehandlingId() == null && mottattDokument.getPayload() != null) {
             dokumentPersistererTjeneste.xmlTilWrapper(mottattDokument);
        }
        innhentDokumentTjeneste.utfør(mottattDokument, behandlingÅrsakType);
    }

    @Override
    protected List<String> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(prosessTaskData.getFagsakId()).stream().map(String::valueOf).collect(Collectors.toList());
    }
}
