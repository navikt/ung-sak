package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;

@ApplicationScoped
@ProsessTask(SendInntektsmeldingForespørselTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SendInntektsmeldingForespørselTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "inntektsmelding.sendForesporsel";
    public static final String ORG_NR = "ORG_NR";
    public static final String SKJÆRINGSTIDSPUNKT = "STP";

    private InntektsmeldingRestKlient inntektsmeldingRestKlient;
    private FagsakRepository fagsakRepository;

    public SendInntektsmeldingForespørselTask() {
    }

    @Inject
    public SendInntektsmeldingForespørselTask(InntektsmeldingRestKlient inntektsmeldingRestKlient, FagsakRepository fagsakRepository) {
        this.inntektsmeldingRestKlient = inntektsmeldingRestKlient;
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsak = fagsakRepository.finnEksaktFagsak(prosessTaskData.getFagsakId());
        inntektsmeldingRestKlient.opprettForespørsel(new OpprettForespørselRequest(
            fagsak.getBrukerAktørId().getAktørId(),
            prosessTaskData.getPropertyValue(ORG_NR),
            LocalDate.parse(prosessTaskData.getPropertyValue(SKJÆRINGSTIDSPUNKT)),
            fagsak.getYtelseType().getKode(),
            fagsak.getSaksnummer().getVerdi()

        ));


    }
}
