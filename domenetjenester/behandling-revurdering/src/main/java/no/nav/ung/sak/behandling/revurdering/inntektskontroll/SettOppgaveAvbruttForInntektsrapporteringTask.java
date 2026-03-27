package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.EndreOppgaveStatusDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Setter oppgave for inntektsrapportering til utløpt.
 */
@ApplicationScoped
@ProsessTask(value = SettOppgaveAvbruttForInntektsrapporteringTask.TASKNAME)
public class SettOppgaveAvbruttForInntektsrapporteringTask implements ProsessTaskHandler {

    public static final String TASKNAME = "inntektsrapportering.settAvbrutt";

    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";

    private UngBrukerdialogOppgaveKlient oppgaveKlient;


    SettOppgaveAvbruttForInntektsrapporteringTask() {
    }

    @Inject
    public SettOppgaveAvbruttForInntektsrapporteringTask(UngBrukerdialogOppgaveKlient oppgaveKlient) {
        this.oppgaveKlient = oppgaveKlient;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var aktørId = new AktørId(prosessTaskData.getAktørId());
        final var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM), DateTimeFormatter.ISO_LOCAL_DATE);
        oppgaveKlient.settOppgaveTilAvbrutt(new EndreOppgaveStatusDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            OppgaveType.RAPPORTER_INNTEKT,
            fom,
            tom
        ));
    }


}
