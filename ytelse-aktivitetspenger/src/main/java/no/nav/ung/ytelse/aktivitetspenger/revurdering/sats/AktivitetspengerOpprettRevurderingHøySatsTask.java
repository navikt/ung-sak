package no.nav.ung.ytelse.aktivitetspenger.revurdering.sats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandling.revurdering.sats.OpprettRevurderingHøySatsTaskTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
@ProsessTask(value = AktivitetspengerOpprettRevurderingHøySatsTask.TASKNAME)
public class AktivitetspengerOpprettRevurderingHøySatsTask implements ProsessTaskHandler {

    public static final String TASKNAME = "opprettRevurderingHøySatsAktivitetspenger";

    private static final Logger log = LoggerFactory.getLogger(AktivitetspengerOpprettRevurderingHøySatsTask.class);
    private AktivitetspengerSatsEndringRepository satsEndringRepository;
    private OpprettRevurderingHøySatsTaskTjeneste tjeneste;

    AktivitetspengerOpprettRevurderingHøySatsTask() {
    }

    @Inject
    public AktivitetspengerOpprettRevurderingHøySatsTask(AktivitetspengerSatsEndringRepository satsEndringRepository, OpprettRevurderingHøySatsTaskTjeneste tjeneste) {
        this.satsEndringRepository = satsEndringRepository;
        this.tjeneste = tjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dato = LocalDate.now();
        log.info("Utleder aktivitetspenger-fagsaker med overgang til høy sats for dato {}", dato.format(DateTimeFormatter.ISO_LOCAL_DATE));
        var fagsakerMedEndringsdato = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(dato);
        tjeneste.opprettRevurderingTasks(prosessTaskData, fagsakerMedEndringsdato);
    }
}
