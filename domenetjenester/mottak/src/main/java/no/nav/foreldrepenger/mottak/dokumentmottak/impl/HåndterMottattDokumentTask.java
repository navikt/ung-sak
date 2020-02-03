package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.DokumentPersistererTjeneste;
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
    private DokumentPersistererTjeneste dokumentPersistererTjeneste;

    HåndterMottattDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public HåndterMottattDokumentTask(BehandlingRepositoryProvider repositoryProvider, 
                                      InnhentDokumentTjeneste innhentDokumentTjeneste,
                                      DokumentPersistererTjeneste dokumentPersistererTjeneste, 
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
        }
        if (prosessTaskData.getBehandlingId() != null) {
            innhentDokumentTjeneste.opprettFraTidligereBehandling(mottattDokument, behandlingÅrsakType);
        } else if (mottattDokument.getPayloadXml() != null) {
             dokumentPersistererTjeneste.xmlTilWrapper(mottattDokument);
        }
        innhentDokumentTjeneste.utfør(mottattDokument, behandlingÅrsakType);
    }

    @Override
    protected List<String> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(prosessTaskData.getFagsakId()).stream().map(String::valueOf).collect(Collectors.toList());
    }
}
