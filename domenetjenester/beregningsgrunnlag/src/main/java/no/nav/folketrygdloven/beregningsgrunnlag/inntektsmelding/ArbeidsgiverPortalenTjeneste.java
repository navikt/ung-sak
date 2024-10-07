package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding.OppdaterForespørslerISakMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding.OppdaterForespørslerISakTask;
import no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding.SendInntektsmeldingForespørselTask;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.etterlysning.BestiltEtterlysning;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.søknad.JsonUtils;

@Dependent
public class ArbeidsgiverPortalenTjeneste {

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private boolean skalSendeForesporsel;

    public ArbeidsgiverPortalenTjeneste() {
    }

    @Inject
    public ArbeidsgiverPortalenTjeneste(ProsessTaskTjeneste prosessTaskTjeneste,
                                        @KonfigVerdi(value = "SEND_INNTEKTSMELDING_FORESPORSEL", defaultVerdi = "false") boolean skalSendeForesporsel) {
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

    public void oppdaterInntektsmeldingforespørslerISak(Map<DatoIntervallEntitet, List<Arbeidsgiver>> forespørselMap, Behandling behandling) {
        if (!skalSendeForesporsel) {
            return;
        }

        var request = OppdaterForespørslerISakMapper.mapTilRequest(forespørselMap, behandling);
        var prosessTaskData = ProsessTaskData.forProsessTask(OppdaterForespørslerISakTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId());
        prosessTaskData.setPayload(JsonUtils.toString(request));
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskTjeneste.lagre(prosessTaskData);
    }
}
