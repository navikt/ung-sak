package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.etterlysning.MidlertidigOppgaveDelegeringTjeneste;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.EndreOppgaveStatusDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Setter oppgave for inntektsrapportering til utløpt.
 */
@ApplicationScoped
@ProsessTask(value = SettOppgaveUtløptForInntektsrapporteringTask.TASKNAME)
public class SettOppgaveUtløptForInntektsrapporteringTask implements ProsessTaskHandler {

    public static final String TASKNAME = "inntektsrapportering.settUtlopt";

    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";

    private MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste;


    SettOppgaveUtløptForInntektsrapporteringTask() {
    }

    @Inject
    public SettOppgaveUtløptForInntektsrapporteringTask(MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste) {

        this.delegeringTjeneste = delegeringTjeneste;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var aktørId = new AktørId(prosessTaskData.getAktørId());
        final var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM), DateTimeFormatter.ISO_LOCAL_DATE);
        delegeringTjeneste.settOppgaveTilUtløpt(new EndreOppgaveStatusDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            OppgaveType.RAPPORTER_INNTEKT,
            fom,
            tom
        ));
    }


}
