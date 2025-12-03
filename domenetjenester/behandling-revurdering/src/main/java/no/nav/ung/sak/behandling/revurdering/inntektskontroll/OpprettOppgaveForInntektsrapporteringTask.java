package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;


/**
 * Task som oppretter oppgave for inntektsrapportering.
 */
@ApplicationScoped
@ProsessTask(value = OpprettOppgaveForInntektsrapporteringTask.TASKNAME)
public class OpprettOppgaveForInntektsrapporteringTask implements ProsessTaskHandler {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");
    private static final Logger LOG = LoggerFactory.getLogger(OpprettOppgaveForInntektsrapporteringTask.class);


    public static final String TASKNAME = "inntektsrapportering.opprettOppgave";

    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";
    public static final String OPPGAVE_REF = "oppgave_ref";

    private PersoninfoAdapter personinfoAdapter;
    private UngOppgaveKlient ungOppgaveKlient;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    private int rapporteringsfristDagIMåned;


    OpprettOppgaveForInntektsrapporteringTask() {
    }

    @Inject
    public OpprettOppgaveForInntektsrapporteringTask(PersoninfoAdapter personinfoAdapter,
                                                     UngOppgaveKlient ungOppgaveKlient,
                                                     FagsakRepository fagsakRepository,
                                                     BehandlingRepository behandlingRepository,
                                                     MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder,
                                                     @KonfigVerdi(value = "INNTEKTSKONTROLL_DAG_I_MAANED", defaultVerdi = "8") int rapporteringsfristDagIMåned) {

        this.personinfoAdapter = personinfoAdapter;
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
        this.rapporteringsfristDagIMåned = rapporteringsfristDagIMåned;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var aktørId = new AktørId(prosessTaskData.getAktørId());
        final var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM), DateTimeFormatter.ISO_LOCAL_DATE);
        if (prosessTaskData.getSaksnummer() != null) {
            logContext(new Saksnummer(prosessTaskData.getSaksnummer()));
            LOG.info("Oppretter oppgave for inntektsrapportering for periode={}", new Periode(fom, tom));
        }
        boolean harIkkeYtelseIHelePerioden = harYtelseIDelAvPerioden(aktørId, fom, tom);
        PersonIdent deltakerIdent = personinfoAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));
        ungOppgaveKlient.opprettInntektrapporteringOppgave(new InntektsrapporteringOppgaveDTO(
            deltakerIdent.getIdent(),
            UUID.fromString(prosessTaskData.getPropertyValue(OPPGAVE_REF)),
            fom.plusMonths(1).withDayOfMonth(rapporteringsfristDagIMåned).atStartOfDay(),
            fom,
            tom,
            harIkkeYtelseIHelePerioden
        ));
    }

    /** Sjekker om bruker ikke har ytelse i hele perioden, men kun i deler av perioden
     * @param aktørId AktlørId til bruker
     * @param fom Første dag i måned til rapportering
     * @param tom Siste dag i måned til rapportering
     * @return
     */
    private boolean harYtelseIDelAvPerioden(AktørId aktørId, LocalDate fom, LocalDate tom) {
        List<Fagsak> fagsaker = fagsakRepository.hentForBruker(aktørId);
        if (fagsaker.isEmpty()) {
            throw new IllegalStateException("Fant ikke fagsak for aktørId=" + aktørId.getId());
        } else if (fagsaker.size() != 1) {
            throw new IllegalStateException("Forventer kun en fagsak for aktørId=" + aktørId.getId() + ", men fant flere: " + fagsaker.stream().map(Fagsak::getSaksnummer).toList());
        }
        var fagsak = fagsaker.get(0);

        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow(() -> new IllegalStateException("Fant ikke behandling"));
        LocalDateTimeline<YearMonth> månedsvisTidslinje = månedsvisTidslinjeUtleder.periodiserMånedsvis(sisteBehandling.getId());
        LocalDateInterval månedForRapportering = new LocalDateInterval(fom, tom);
        return overlapperPeriodeDelvisMedProgramtidslinje(månedForRapportering, månedsvisTidslinje);
    }

    private static <T> boolean overlapperPeriodeDelvisMedProgramtidslinje(LocalDateInterval periode, LocalDateTimeline<T> programtidslinje) {
        LocalDateTimeline<Boolean> periodeSomTidslinje = new LocalDateTimeline<>(periode, true);
        LocalDateTimeline<T> overlapp = programtidslinje.intersection(periode);
        LocalDateTimeline<Boolean> periodeEtterFjernetOverlapp = periodeSomTidslinje.disjoint(overlapp);
        return !periodeEtterFjernetOverlapp.isEmpty();
    }
    /** log mdc cleares automatisk når task har kjørt, så trenger ikke kalle clearLogContext. */
    public static void logContext(Saksnummer saksnummer) {
        LOG_CONTEXT.add("saksnummer", saksnummer);
    }


}
