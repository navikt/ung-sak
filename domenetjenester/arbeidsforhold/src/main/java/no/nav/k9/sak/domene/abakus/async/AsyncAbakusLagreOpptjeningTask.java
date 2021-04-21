package no.nav.k9.sak.domene.abakus.async;

import java.io.IOException;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.domene.abakus.AbakusTjeneste;

@ApplicationScoped
@ProsessTask(AsyncAbakusLagreOpptjeningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
class AsyncAbakusLagreOpptjeningTask extends UnderBehandlingProsessTask {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAbakusLagreOpptjeningTask.class);

    public static final String TASKTYPE = "abakus.async.lagreopptjening";

    /**
     * Angir hvilken type opptjening som skal lagres. (deprecated, bruk opptjening.type)
     */
    private static final String LAGRE_OVERSTYRT = "opptjening.overstyrt";

    /**
     * Angir hvilken type opptjening som skal lagres.
     */
    static final String OPPTJENINGSTYPE = "opptjening.type";

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
        //TODO navn på property endres, leser her både ny og gammel for å håndtere prosesstasks som er oppprettet under gammelt regime
        //TODO når alle gamle prosesstasker er ferdige, holder det å lese fra opptjening.type
        String deprecatedProperty = input.getPropertyValue(LAGRE_OVERSTYRT);
        String nyProperty = input.getPropertyValue(OPPTJENINGSTYPE);
        String konfigurertVerdi = nyProperty != null ? nyProperty : deprecatedProperty;
        if (deprecatedProperty != null) {
            logger.warn("Prosesstask med id {} og type {} som bruker property opptjening.overstyrt. Kan ikke rydde deprecated kode i {} enda", input.getId(), TASKTYPE, AsyncAbakusLagreOpptjeningTask.class.getSimpleName());
        }

        var opptjeningType = AsyncInntektArbeidYtelseTjeneste.OpptjeningType.valueOf(Objects.requireNonNull(konfigurertVerdi, "Både opptjening.overstyrt og opptjening.type mangler"));
        var jsonReader = IayGrunnlagJsonMapper.getMapper().readerFor(OppgittOpptjeningMottattRequest.class);

        try {
            OppgittOpptjeningMottattRequest request = jsonReader.readValue(Objects.requireNonNull(input.getPayloadAsString(), "mangler payload"));
            switch (opptjeningType) {
                case NORMAL:
                    abakusTjeneste.lagreOppgittOpptjening(request);
                    break;
                case NORMAL_AGGREGAT:
                    abakusTjeneste.lagreOppgittOpptjeningV2(request);
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
