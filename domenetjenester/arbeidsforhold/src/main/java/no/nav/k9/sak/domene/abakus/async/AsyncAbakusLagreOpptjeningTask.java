package no.nav.k9.sak.domene.abakus.async;

import java.io.IOException;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.domene.abakus.AbakusTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(AsyncAbakusLagreOpptjeningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
class AsyncAbakusLagreOpptjeningTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "abakus.async.lagreopptjening";

    /** Angir hvorvidt det er overstyrt opptjening som skal lagres. */
    static final String LAGRE_OVERSTYRT = "opptjening.overstyrt";

    private AbakusTjeneste abakusTjeneste;

    AsyncAbakusLagreOpptjeningTask() {
        // for proxy
    }

    @Inject
    AsyncAbakusLagreOpptjeningTask(BehandlingRepository behandlingRepository, BehandlingLåsRepository behandlingLåsRepository, AbakusTjeneste abakusTjeneste) {
        super(behandlingRepository, behandlingLåsRepository);
        this.abakusTjeneste = abakusTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData input, Behandling tilBehandling) {

        var opptjeningType = AsyncInntektArbeidYtelseTjeneste.OpptjeningType.valueOf(Objects.requireNonNull(input.getPropertyValue(LAGRE_OVERSTYRT), LAGRE_OVERSTYRT));
        var jsonReader = IayGrunnlagJsonMapper.getMapper().readerFor(OppgittOpptjeningMottattRequest.class);

        try {
            OppgittOpptjeningMottattRequest request = jsonReader.readValue(Objects.requireNonNull(input.getPayloadAsString(), "mangler payload"));
            switch (opptjeningType) {
                case NORMAL:
                    abakusTjeneste.lagreOppgittOpptjening(request);
                    break;
                case OVERSTYRT:
                    abakusTjeneste.lagreOverstyrtOppgittOpptjening(request);
                    break;
                default:
                    throw new UnsupportedOperationException("Støtter ikke opptjening type: " + opptjeningType);
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Kunne ikke lagre abakus oppgitt opptjening [%s]", opptjeningType), e);
        }
    }
}
