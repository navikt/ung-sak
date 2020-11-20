package no.nav.k9.sak.domene.abakus.async;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerMottattRequest;
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
@ProsessTask(AsyncAbakusLagreTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AsyncAbakusLagreTask extends UnderBehandlingProsessTask {
    static final String TASKTYPE = "abakus.async.lagre";
    static final String KEY = "action";

    private static final ObjectMapper MAPPER = IayGrunnlagJsonMapper.getMapper();

    private AbakusTjeneste abakusTjeneste;

    public enum Action {
        LAGRE_OPPGITT_OPPTJENING(OppgittOpptjeningMottattRequest.class),
        LAGRE_INNTEKTSMELDINGER(InntektsmeldingerMottattRequest.class),
        LAGRE_OVERSTYRT_OPPTJENING(OppgittOpptjeningMottattRequest.class),
        ;

        @SuppressWarnings("rawtypes")
        private final Class forventetType;

        @SuppressWarnings("rawtypes")
        Action(Class forventetType) {
            this.forventetType = forventetType;
        }

        @SuppressWarnings("unchecked")
        public <V> Class<V> getForventetType() {
            return forventetType;
        }

        void validerForventetType(Object obj) {
            if (!forventetType.isInstance(obj)) {
                throw new IllegalArgumentException("Angitt objekt er ikke av type " + forventetType.getName() + ": " + obj);
            }
        }
    }

    AsyncAbakusLagreTask() {
        // for proxy
    }

    @Inject
    public AsyncAbakusLagreTask(BehandlingRepository behandlingRepository, BehandlingLåsRepository behandlingLåsRepository, AbakusTjeneste abakusTjeneste) {
        super(behandlingRepository, behandlingLåsRepository);
        this.abakusTjeneste = abakusTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData input, Behandling behandling) {

        String behandlingId = input.getBehandlingId();
        Action action = Action.valueOf(input.getPropertyValue(KEY));
        String payload = input.getPayloadAsString();


        try {
            var ref = behandling.getUuid();
            switch (action) {
                case LAGRE_OPPGITT_OPPTJENING:
                    abakusTjeneste.lagreOppgittOpptjening(ref, payload);
                case LAGRE_OVERSTYRT_OPPTJENING:
                    abakusTjeneste.lagreOverstyrtOppgittOpptjening(ref, payload);
                case LAGRE_INNTEKTSMELDINGER:
                    abakusTjeneste.lagreInntektsmeldinger(ref, payload);
                default:
                    throw new UnsupportedOperationException("Støtter ikke action: " + action);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Kan ikke utføre " + action + " for behandling=" + behandlingId, e);
        }

    }

    static <V> V readValue(String payload, Class<V> cls) {
        try {
            return MAPPER.readValue(payload, cls);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("kunne ikke deserialisere payload:" + payload, e);
        }
    }

    static void initPayload(ProsessTaskData data, Action action, Object payload) {
        try {
            action.validerForventetType(payload);
            String payloadString = MAPPER.writeValueAsString(payload);
            data.setPayload(payloadString);
            data.setProperty(KEY, action.name());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("kunne ikke serialisere payload:" + payload, e);
        }
    }

}
