package no.nav.ung.domenetjenester.oppgave.gosys;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.handler.WrappedProsessTaskHandler;
import no.nav.ung.fordel.kodeverdi.GosysKonstanter;
import no.nav.ung.fordel.repo.MeldingRepository;
import no.nav.ung.fordel.repo.ProduksjonsstyringOppgaveEntitet;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ProsessTask(OpprettOppgaveTask.TASKTYPE)
public class OpprettOppgaveTask extends WrappedProsessTaskHandler {

    public static final String TASKTYPE = "oppgave.opprettOppgave";

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveTask.class);

    private GosysOppgaveService gosysOppgaveService;
    private MeldingRepository meldingRepository;

    private JournalpostRepository journalpostRepository;

    @Inject
    public OpprettOppgaveTask(FordelProsessTaskTjeneste fordelProsessTaskTjeneste,
                              GosysOppgaveService gosysOppgaveService,
                              JournalpostRepository journalpostRepository,
                              MeldingRepository meldingRepository) {
        super(fordelProsessTaskTjeneste);
        this.gosysOppgaveService = gosysOppgaveService;
        this.journalpostRepository = journalpostRepository;
        this.meldingRepository = meldingRepository;
    }

    @Override
    public void precondition(MottattMelding data) {
        if (data.getJournalPostIder().size() != 1) {
            throw new IllegalStateException("Støtter ikke å opprettet oppgave i gosys for søknad med flere journalposter eller ingen p.d.d.");
        }
    }

    @Override
    public MottattMelding doTask(MottattMelding data) {

        try {


            var oppgaveType = data.getOppgaveType().orElse(GosysKonstanter.OppgaveType.JOURNALFØRING);

            var søkerAktørId = data.getAktørId().map(AktørId::new).orElse(null);

            if (data.getJournalPostIder().size() > 1) {
                throw new UnsupportedOperationException("Støtter bare opprette oppgave for en journalpost av gangen, fikk: " + data.getJournalPostIder());
            }
            var journalpostId = data.getJournalPostIder().iterator().next();

            var beskrivelse = data.getBeskrivelse().orElse(null);
            var tema = data.getTema();
            var behandlingTema = data.getBehandlingTema();
            var behandlingType = data.getBehandlingType();

            log.info("Oppretter oppgave: oppgaveType={}, tema={}, behandingTema={}, behandlingType={}: {}",
                oppgaveType, tema, behandlingTema, behandlingType, beskrivelse);

            // marker mottatt journalpost som behandlet
            journalpostRepository.markerJournalposterBehandlet(journalpostId);

            // spor oppgave data
            var oppgave = lagreOppgaveEntitet(data, oppgaveType, journalpostId, beskrivelse);


            /*
             * Det å sette fagsystem sperrer muligheten for å løse saken i Gosys.
             * Oppgavene her er bare de som manuelt må behandles i Gosys da vi ikke har punsj.
             *
             */
            final GosysKonstanter.Fagsaksystem fagsaksystem = null;

            // opprett gosys oppgave og få tildelt oppgaveid
            var gosysOppgaveId = gosysOppgaveService.opprettEllerHentEksisterendeOppgave(
                tema,
                behandlingTema,
                behandlingType.orElse(null),
                søkerAktørId,
                journalpostId,
                beskrivelse,
                fagsaksystem,
                oppgaveType);

            log.info("Opprettet oppgave={}", gosysOppgaveId);

            // oppdater sporede data med oppgaveId
            oppgave.setOppgaveId(gosysOppgaveId);
            lagreOppgaveEntitet(oppgave);

            return null;
        } catch (Exception e) {
            log.warn("Uventet feil ved kjøring av OpprettOppgaveTask", e);
            throw e;
        }
    }

    private ProduksjonsstyringOppgaveEntitet lagreOppgaveEntitet(MottattMelding data, GosysKonstanter.OppgaveType oppgaveType,
                                                                 JournalpostId journalpostId, String beskrivelse) {
        // sporer opprette oppgave
        var ytelseType = data.getYtelseType().orElse(null);
        var oppgaveEntitet = new ProduksjonsstyringOppgaveEntitet.Builder()
            .withAktørId(data.getAktørId().orElse(null))
            .withJournalpostId(journalpostId.getVerdi())
            .withBeskrivelse(beskrivelse)
            .withOppgaveType(oppgaveType)
            .withYtelseType(ytelseType)
            .withBehandlingTema(data.getBehandlingTema())
            .build();
        lagreOppgaveEntitet(oppgaveEntitet);
        return oppgaveEntitet;
    }

    private void lagreOppgaveEntitet(ProduksjonsstyringOppgaveEntitet oppgaveEntitet) {
        meldingRepository.lagre(oppgaveEntitet);
    }
}
