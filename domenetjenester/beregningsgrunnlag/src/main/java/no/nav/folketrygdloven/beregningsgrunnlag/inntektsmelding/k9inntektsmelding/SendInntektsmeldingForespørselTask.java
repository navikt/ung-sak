package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
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
            new AktørIdDto(fagsak.getBrukerAktørId().getAktørId()),
            new OrganisasjonsnummerDto(prosessTaskData.getPropertyValue(ORG_NR)),
            LocalDate.parse(prosessTaskData.getPropertyValue(SKJÆRINGSTIDSPUNKT)),
            finnYtelseType(fagsak),
            new SaksnummerDto(fagsak.getSaksnummer().getVerdi())

        ));


    }

    private static YtelseType finnYtelseType(Fagsak fagsak) {
        return switch (fagsak.getYtelseType()) {
            case OMSORGSPENGER -> YtelseType.OMSORGSPENGER;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case PLEIEPENGER_SYKT_BARN -> YtelseType.PLEIEPENGER_SYKT_BARN;
            case OPPLÆRINGSPENGER -> YtelseType.OPPLÆRINGSPENGER;
            default -> throw new IllegalStateException("Unexpected value: " + fagsak.getYtelseType());
        };
    }
}
