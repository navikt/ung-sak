package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding;

import java.time.format.DateTimeFormatter;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.etterlysning.BestiltEtterlysning;

@ApplicationScoped
public class ArbeidsgiverPortalenTjeneste {

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private boolean skalSendeForesporsel;

    public ArbeidsgiverPortalenTjeneste() {
    }

    @Inject
    public ArbeidsgiverPortalenTjeneste(ProsessTaskTjeneste prosessTaskTjeneste, @KonfigVerdi(value = "SEND_INNTEKTSMELDING_FORESPORSEL", defaultVerdi = "false") boolean skalSendeForesporsel) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.skalSendeForesporsel = skalSendeForesporsel;
    }

    public void sendInntektsmeldingForespørsel(Set<BestiltEtterlysning> bestiltEtterlysninger) {
        if (!skalSendeForesporsel) {
            return;
        }

        bestiltEtterlysninger.stream().filter(BestiltEtterlysning::getErArbeidsgiverMottaker)
            .filter(e -> e.getArbeidsgiver().getErVirksomhet())
            .forEach(e -> {
                var prosessTaskData = ProsessTaskData.forProsessTask(SendInntektsmeldingForespørselTask.class);
                prosessTaskData.setFagsakId(e.getFagsakId());
                prosessTaskData.setProperty(SendInntektsmeldingForespørselTask.ORG_NR, e.getArbeidsgiver().getArbeidsgiverOrgnr());
                prosessTaskData.setProperty(SendInntektsmeldingForespørselTask.SKJÆRINGSTIDSPUNKT, e.getPeriode().getFomDato().format(DateTimeFormatter.ISO_LOCAL_DATE));
                prosessTaskTjeneste.lagre(prosessTaskData);
            });
    }

}
