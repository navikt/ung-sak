package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.søknad.JsonUtils;

@ApplicationScoped
@ProsessTask(OppdaterForespørslerISakTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OppdaterForespørslerISakTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "inntektsmelding.oppdaterSak";

    private InntektsmeldingRestKlient inntektsmeldingRestKlient;

    public OppdaterForespørslerISakTask() {
    }

    @Inject
    public OppdaterForespørslerISakTask(InntektsmeldingRestKlient inntektsmeldingRestKlient) {
        this.inntektsmeldingRestKlient = inntektsmeldingRestKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var request = JsonUtils.fromString(prosessTaskData.getPayloadAsString(), OppdaterForespørslerISakRequest.class);
        inntektsmeldingRestKlient.oppdaterSak(request);
    }
}
