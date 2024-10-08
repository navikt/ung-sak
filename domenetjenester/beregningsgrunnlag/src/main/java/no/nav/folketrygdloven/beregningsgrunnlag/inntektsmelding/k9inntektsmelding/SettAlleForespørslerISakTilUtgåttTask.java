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
@ProsessTask(SettAlleForespørslerISakTilUtgåttTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettAlleForespørslerISakTilUtgåttTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "inntektsmelding.settAlleForespørslerISakTilUtgått";

    private static final Logger log = LoggerFactory.getLogger(SettAlleForespørslerISakTilUtgåttTask.class);

    private InntektsmeldingRestKlient inntektsmeldingRestKlient;

    public SettAlleForespørslerISakTilUtgåttTask() {
    }

    @Inject
    public SettAlleForespørslerISakTilUtgåttTask(InntektsmeldingRestKlient inntektsmeldingRestKlient) {
        this.inntektsmeldingRestKlient = inntektsmeldingRestKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var request = JsonUtils.fromString(prosessTaskData.getPayloadAsString(), SettAlleForespørslerTilUtgåttRequest.class);
        log.info("Setter inntektsmeldingforespørsler for sak {} som ikke er behandlet til utgått", request.fagsakSaksnummer().saksnr());
        inntektsmeldingRestKlient.settAlleForespørslerTilUtgått(request);
    }
}
