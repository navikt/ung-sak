package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.SettTilUtløptDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Batchtask som starter kontroll av inntekt fra a-inntekt
 * <p>
 * Kjører hver dag kl 07:15.
 */
@ApplicationScoped
@ProsessTask(value = SettOppgaveUtløptForInntektsrapporteringTask.TASKNAME, cronExpression = "0 0 7 1 * *", maxFailedRuns = 1)
public class SettOppgaveUtløptForInntektsrapporteringTask implements ProsessTaskHandler {

    public static final String TASKNAME = "batch.settUtloptForInntektsrapportering";

    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";

    private static final Logger log = LoggerFactory.getLogger(SettOppgaveUtløptForInntektsrapporteringTask.class);

    private PersoninfoAdapter personinfoAdapter;
    private UngOppgaveKlient ungOppgaveKlient;


    SettOppgaveUtløptForInntektsrapporteringTask() {
    }

    @Inject
    public SettOppgaveUtløptForInntektsrapporteringTask(PersoninfoAdapter personinfoAdapter,
                                                        UngOppgaveKlient ungOppgaveKlient) {

        this.personinfoAdapter = personinfoAdapter;
        this.ungOppgaveKlient = ungOppgaveKlient;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var aktørId = new AktørId(prosessTaskData.getAktørId());
        final var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM), DateTimeFormatter.ISO_LOCAL_DATE);
        log.info("Setter oppgave for inntektsrapportering til utløpt for aktørId {} fra {} til {}", aktørId, fom, tom);
        PersonIdent deltakerIdent = personinfoAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));
        ungOppgaveKlient.settOppgaveTilUtløpt(new SettTilUtløptDTO(
            deltakerIdent.getIdent(),
            Oppgavetype.RAPPORTER_INNTEKT,
            fom,
            tom
        )); // TODO: Ta i bruk egen requestdto for utløp av inntektsrapportering
    }


}
