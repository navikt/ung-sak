package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

@ApplicationScoped
@ProsessTask(SettÅpneImForespørslerTilUtgåttTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettÅpneImForespørslerTilUtgåttTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.settÅpneImForespørslerTilUtgått";

    private static final Logger log = LoggerFactory.getLogger(SettÅpneImForespørslerTilUtgåttTask.class);

    private InntektsmeldingRestKlient inntektsmeldingRestKlient;

    public SettÅpneImForespørslerTilUtgåttTask() {
    }

    @Inject
    public SettÅpneImForespørslerTilUtgåttTask(InntektsmeldingRestKlient inntektsmeldingRestKlient) {
        this.inntektsmeldingRestKlient = inntektsmeldingRestKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String saksnummer = prosessTaskData.getSaksnummer();
        log.info("Setter inntektsmeldingforespørsler for sak {} som ikke er behandlet til utgått", saksnummer);
        inntektsmeldingRestKlient.settAlleÅpneForespørslerTilUtgått(saksnummer);
    }
}
