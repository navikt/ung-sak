package no.nav.k9.sak.domene.abakus.async;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.typer.AktørId;

/**
 * Kaller kopier grunnlag i abakus i egen task (som gjør at feil som kan oppstå i det kallet håndteres isolert her).
 */
@Dependent
public class AsyncInntektArbeidYtelseTjeneste {

    private ProsessTaskTjeneste prosessTaskRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public AsyncInntektArbeidYtelseTjeneste(BehandlingRepository behandlingRepository,
                                            ProsessTaskTjeneste prosessTaskRepository) {
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void kopierIayGrunnlag(Long originalBehandlingId, Long targetBehandlingId, Set<Dataset> dataset) {
        var behandling = behandlingRepository.hentBehandling(targetBehandlingId);
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
        AsyncAbakusKopierGrunnlagTask.preconditions(originalBehandling, behandling);

        AktørId aktørId = behandling.getAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        var enkeltTask = ProsessTaskData.forProsessTask(AsyncAbakusKopierGrunnlagTask.class);
        enkeltTask.setCallIdFraEksisterende();
        enkeltTask.setBehandling(behandling.getFagsakId(), targetBehandlingId, aktørId.getId());
        enkeltTask.setSaksnummer(saksnummer.getVerdi());
        enkeltTask.setProperty(AsyncAbakusKopierGrunnlagTask.ORIGINAL_BEHANDLING_ID, originalBehandlingId.toString());

        // hvilke dataset vi kopierer med oss
        if (dataset != null && !Objects.equals(EnumSet.allOf(Dataset.class), dataset)) {
            // tar bare med hvis satt noe annet enn alle
            List<String> datasetStringKoder = dataset.stream().map(Dataset::name).collect(Collectors.toList());
            enkeltTask.setProperty(AsyncAbakusKopierGrunnlagTask.DATASET, String.join(",", datasetStringKoder));
        }

        prosessTaskRepository.lagre(enkeltTask);
    }

}
