package no.nav.k9.sak.domene.abakus.async;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.abakus.async.AsyncAbakusLagreTask.Action;
import no.nav.k9.sak.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@Dependent
public class AsyncInntektArbeidYtelseTjeneste {

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public AsyncInntektArbeidYtelseTjeneste(BehandlingRepository behandlingRepository,
                                            ProsessTaskRepository prosessTaskRepository) {
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder) {
        if (oppgittOpptjeningBuilder == null) {
            return;
        }
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        AktørId aktørId = behandling.getAktørId();
        var aktør = new AktørIdPersonident(aktørId.getId());
        var saksnummer = behandling.getFagsak().getSaksnummer();
        var ytelseType = YtelseType.fraKode(behandling.getFagsakYtelseType().getKode());
        var oppgittOpptjening = new IAYTilDtoMapper(aktørId, null, behandling.getUuid()).mapTilDto(oppgittOpptjeningBuilder);
        var request = new OppgittOpptjeningMottattRequest(saksnummer.getVerdi(), behandling.getUuid(), aktør, ytelseType, oppgittOpptjening);

        var enkeltTask = new ProsessTaskData(AsyncAbakusLagreTask.TASKTYPE);
        enkeltTask.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        enkeltTask.setSaksnummer(saksnummer.getVerdi());
        enkeltTask.setCallIdFraEksisterende();

        AsyncAbakusLagreTask.initPayload(enkeltTask, Action.LAGRE_OPPGITT_OPPTJENING, request);

        prosessTaskRepository.lagre(enkeltTask);
    }

}
