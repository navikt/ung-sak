package no.nav.k9.sak.domene.abakus.async;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.k9.sak.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

/** Kaller kopier grunnlag i abakus i egen task (som gjør at feil som kan oppstå i det kallet håndteres isolert her). */
@Dependent
public class AsyncInntektArbeidYtelseTjeneste {

    static enum OpptjeningType {
        NORMAL,
        OVERSTYRT
    }

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public AsyncInntektArbeidYtelseTjeneste(BehandlingRepository behandlingRepository,
                                     ProsessTaskRepository prosessTaskRepository) {
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void kopierIayGrunnlag(Long originalBehandlingId, Long targetBehandlingId, Set<Dataset> dataset) {
        var behandling = behandlingRepository.hentBehandling(targetBehandlingId);
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
        AsyncAbakusKopierGrunnlagTask.preconditions(originalBehandling, behandling);

        AktørId aktørId = behandling.getAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        var enkeltTask = new ProsessTaskData(AsyncAbakusKopierGrunnlagTask.TASKTYPE);
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

    public void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder, boolean overstyrt) {
        if (oppgittOpptjeningBuilder == null) {
            return;
        }
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var aktør = new AktørIdPersonident(behandling.getAktørId().getId());
        var saksnummer = behandling.getFagsak().getSaksnummer();
        var ytelseType = YtelseType.fraKode(behandling.getFagsakYtelseType().getKode());
        var oppgittOpptjening = new IAYTilDtoMapper(behandling.getAktørId(), null, behandling.getUuid()).mapTilDto(oppgittOpptjeningBuilder);
        var request = new OppgittOpptjeningMottattRequest(saksnummer.getVerdi(), behandling.getUuid(), aktør, ytelseType, oppgittOpptjening);

        try {
            var enkeltTask = new ProsessTaskData(AsyncAbakusLagreOpptjeningTask.TASKTYPE);
            enkeltTask.setCallIdFraEksisterende();
            enkeltTask.setBehandling(behandling.getFagsakId(), behandlingId, aktør.getIdent());
            enkeltTask.setSaksnummer(saksnummer.getVerdi());
            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.LAGRE_OVERSTYRT, (overstyrt ? OpptjeningType.OVERSTYRT : OpptjeningType.NORMAL).name());

            var payload = IayGrunnlagJsonMapper.getMapper().writeValueAsString(request);
            enkeltTask.setPayload(payload);

            prosessTaskRepository.lagre(enkeltTask);

        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Lagre oppgitt opptjening i abakus feiler.", e).toException();
        }
    }

}
