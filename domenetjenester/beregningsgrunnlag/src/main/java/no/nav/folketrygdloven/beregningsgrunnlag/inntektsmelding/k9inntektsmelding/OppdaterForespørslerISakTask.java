package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(OppdaterForespørslerISakTask.class);

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
        log.info("Oppdaterer inntektsmeldingforespørsler for sak {} med skjæringstidspunkt: {}", request.fagsakSaksnummer().saksnr(), request.skjæringstidspunkterPerOrganisasjon().keySet());
        inntektsmeldingRestKlient.oppdaterSak(request);
    }
}
