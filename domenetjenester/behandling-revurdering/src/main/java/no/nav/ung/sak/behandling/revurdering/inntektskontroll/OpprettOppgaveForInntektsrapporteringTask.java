package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.exception.IntegrasjonException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.etterlysning.UngOppgavetjenesteFeil;
import no.nav.ung.sak.kontrakt.FeilDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


/**
 * Task som oppretter oppgave for inntektsrapportering.
 */
@ApplicationScoped
@ProsessTask(value = OpprettOppgaveForInntektsrapporteringTask.TASKNAME)
public class OpprettOppgaveForInntektsrapporteringTask extends FagsakProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(OpprettOppgaveForInntektsrapporteringTask.class);

    public static final String TASKNAME = "inntektsrapportering.opprettOppgave";

    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";
    public static final String OPPGAVE_REF = "oppgave_ref";

    private PersoninfoAdapter personinfoAdapter;
    private UngOppgaveKlient ungOppgaveKlient;
    private int rapporteringsfristDagIMåned;


    OpprettOppgaveForInntektsrapporteringTask() {
    }

    @Inject
    public OpprettOppgaveForInntektsrapporteringTask(PersoninfoAdapter personinfoAdapter,
                                                     UngOppgaveKlient ungOppgaveKlient,
                                                     @KonfigVerdi(value = "RAPPORTERINGSFRIST_DAG_I_MAANED", defaultVerdi = "6") int rapporteringsfristDagIMåned) {

        this.personinfoAdapter = personinfoAdapter;
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.rapporteringsfristDagIMåned = rapporteringsfristDagIMåned;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var aktørId = new AktørId(prosessTaskData.getAktørId());
        final var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM), DateTimeFormatter.ISO_LOCAL_DATE);
        if (prosessTaskData.getSaksnummer() != null) {
            LOG.info("Oppretter oppgave for inntektsrapportering for aktør knyttet til saksnummer={} for periode={}", prosessTaskData.getSaksnummer(), new Periode(fom,tom));
        }
        PersonIdent deltakerIdent = personinfoAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));
        ungOppgaveKlient.opprettInntektrapporteringOppgave(new InntektsrapporteringOppgaveDTO(
            deltakerIdent.getIdent(),
            UUID.fromString(prosessTaskData.getPropertyValue(OPPGAVE_REF)),
            fom.plusMonths(1).withDayOfMonth(rapporteringsfristDagIMåned + 1).atStartOfDay(),
            fom,
            tom
        ));
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        final var aktørId = new AktørId(prosessTaskData.getAktørId());
        final var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM), DateTimeFormatter.ISO_LOCAL_DATE);
        if (prosessTaskData.getSaksnummer() != null) {
            LOG.info("Oppretter oppgave for inntektsrapportering for aktør knyttet til saksnummer={} for periode={}", prosessTaskData.getSaksnummer(), new Periode(fom,tom));
        }
        PersonIdent deltakerIdent = personinfoAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));
        ungOppgaveKlient.opprettInntektrapporteringOppgave(new InntektsrapporteringOppgaveDTO(
            deltakerIdent.getIdent(),
            UUID.fromString(prosessTaskData.getPropertyValue(OPPGAVE_REF)),
            fom.plusMonths(1).withDayOfMonth(rapporteringsfristDagIMåned + 1).atStartOfDay(),
            fom,
            tom
        ));
    }

}
