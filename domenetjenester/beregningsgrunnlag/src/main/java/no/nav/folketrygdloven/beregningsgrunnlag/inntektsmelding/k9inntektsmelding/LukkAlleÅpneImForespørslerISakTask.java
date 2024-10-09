package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;

@ApplicationScoped
@ProsessTask(LukkAlleÅpneImForespørslerISakTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class LukkAlleÅpneImForespørslerISakTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.lukkÅpneImForespørsler";

    private static final Logger log = LoggerFactory.getLogger(LukkAlleÅpneImForespørslerISakTask.class);

    private InntektsmeldingRestKlient inntektsmeldingRestKlient;

    public LukkAlleÅpneImForespørslerISakTask() {
    }

    @Inject
    public LukkAlleÅpneImForespørslerISakTask(InntektsmeldingRestKlient inntektsmeldingRestKlient) {
        this.inntektsmeldingRestKlient = inntektsmeldingRestKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String saksnummer = prosessTaskData.getSaksnummer();
        log.info("Setter inntektsmeldingforespørsler for sak {} som ikke er behandlet til utgått", saksnummer);
        inntektsmeldingRestKlient.lukkAlleÅpneForespørsler(new SaksnummerDto(saksnummer));
    }
}
