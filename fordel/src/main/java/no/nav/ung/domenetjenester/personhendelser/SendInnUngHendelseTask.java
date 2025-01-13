package no.nav.ung.domenetjenester.personhendelser;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.fordel.repo.hendelser.HendelseRepository;
import no.nav.ung.fordel.repo.hendelser.InngåendeHendelseEntitet;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelsemottakTjeneste;
import no.nav.ung.sak.kontrakt.hendelser.HendelseDto;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
@ProsessTask(SendInnUngHendelseTask.TASKNAME)
public class SendInnUngHendelseTask implements ProsessTaskHandler {

    public static final String TASKNAME = "ung.hendelseInnsendelse";
    public static final String INNGÅENDE_HENDELSE_ID = "hendelse.ihId";

    private static final Logger log = LoggerFactory.getLogger(SendInnUngHendelseTask.class);

    private HendelseRepository hendelseRepository;
    private HendelsemottakTjeneste hendelsemottakTjeneste;

    public SendInnUngHendelseTask() {
        // CDI
    }

    @Inject
    public SendInnUngHendelseTask(HendelseRepository hendelseRepository, HendelsemottakTjeneste hendelsemottakTjeneste) {
        this.hendelseRepository = hendelseRepository;
        this.hendelsemottakTjeneste = hendelsemottakTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var inngåendeHendelseId = Long.valueOf(prosessTaskData.getPropertyValue(INNGÅENDE_HENDELSE_ID));
        var inngåendeHendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelseId);
        if (inngåendeHendelse.getHåndtertStatus() == InngåendeHendelseEntitet.HåndtertStatusType.HÅNDTERT) {
            return;
        }

        var alleUhåndterteHendelser = hendelseRepository.finnUhåndterteHendelser(inngåendeHendelse);
        var sisteUhåndterte = hentSisteInngåendeHendelse(alleUhåndterteHendelser);
        if (!sisteUhåndterte.getId().equals(inngåendeHendelseId)) {
            log.info("Oppdatering fra hendelseId={} blir gruppert med senere mottatt hendelseId={}", inngåendeHendelseId, sisteUhåndterte.getId());
            return;
        }

        // Siste hendelse antas å representere mest relevant informasjon sammenlignet med tidligere hendelser
        log.info("Oppdaterer med hendelseId={}, antall relaterte hendelser={}", inngåendeHendelseId, alleUhåndterteHendelser.size() - 1);
        var aktørId = new AktørId(inngåendeHendelse.getAktørId());
        var hendelse = HåndterUngSakHendelseTask.fraJson(sisteUhåndterte.getPayload());
        var dto = new HendelseDto(hendelse, aktørId);
        hendelsemottakTjeneste.mottaHendelse(hendelse);

        // Alle anses som håndterte etter dette, og får status oppdatert som én gruppe
        var håndtertAv = sisteUhåndterte.getHendelseId();
        alleUhåndterteHendelser.forEach(uhåndtert ->
            hendelseRepository.oppdaterHåndtertStatus(uhåndtert, håndtertAv, InngåendeHendelseEntitet.HåndtertStatusType.HÅNDTERT));
    }

    private InngåendeHendelseEntitet hentSisteInngåendeHendelse(List<InngåendeHendelseEntitet> alleUhåndterteHendelser) {
        if (alleUhåndterteHendelser.isEmpty()) {
            throw new IllegalStateException("Skal ha minst én uhåndtert hendelse");
        }
        var sisteUhåndterte = alleUhåndterteHendelser.get(0);
        return sisteUhåndterte;
    }
}
