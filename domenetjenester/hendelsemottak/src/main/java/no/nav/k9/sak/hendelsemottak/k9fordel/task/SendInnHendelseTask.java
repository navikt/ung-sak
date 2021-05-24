package no.nav.k9.sak.hendelsemottak.k9fordel.task;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.hendelser.HåndtertStatusType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.hendelsemottak.k9fordel.domene.HendelseRepository;
import no.nav.k9.sak.hendelsemottak.k9fordel.domene.InngåendeHendelse;
import no.nav.k9.sak.hendelsemottak.resttjenester.FordelHendelseRestTjeneste;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.kontrakt.hendelser.JsonMapper;


@ApplicationScoped
@ProsessTask(SendInnHendelseTask.TASKNAME)
public class SendInnHendelseTask implements ProsessTaskHandler {

    public static final String TASKNAME = "k9.hendelseInnsendelse";
    public static final String INNGÅENDE_HENDELSE_ID = "hendelse.ihId";

    private static final Logger log = LoggerFactory.getLogger(SendInnHendelseTask.class);

    private HendelseRepository hendelseRepository;
    private FordelHendelseRestTjeneste fordelRestTjeneste;

    public SendInnHendelseTask() {
        // CDI
    }

    @Inject
    public SendInnHendelseTask(HendelseRepository hendelseRepository,
                               FordelHendelseRestTjeneste fordelHendelseRestTjeneste) {
        this.hendelseRepository = hendelseRepository;
        this.fordelRestTjeneste = fordelHendelseRestTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var inngåendeHendelseId = Long.valueOf(prosessTaskData.getPropertyValue(INNGÅENDE_HENDELSE_ID));
        var inngåendeHendelse = hendelseRepository.finnEksaktHendelse(inngåendeHendelseId);
        if (inngåendeHendelse.getHåndtertStatus() == HåndtertStatusType.HÅNDTERT) {
            return;
        }

        var alleUhåndterteHendelser = hendelseRepository.finnUhåndterteHendelser(inngåendeHendelse);
        var førsteUhåndterte = hentFørsteInngåendeHendelse(alleUhåndterteHendelser);
        if (!førsteUhåndterte.getId().equals(inngåendeHendelseId)) {
            log.info("Oppdatering fra hendelseId={} blir gruppert med tidligere mottatt hendelseId={}", inngåendeHendelseId, førsteUhåndterte.getId());
            return;
        }

        // Første hendelse antas å representere samme relevant informasjon som evt. etterfølgende hendelser
        log.info("Oppdaterer med hendelseId={}, antall relaterte hendelser={}", inngåendeHendelseId, alleUhåndterteHendelser.size() - 1);
        var førsteHendelseInnhold = JsonMapper.fromJson(inngåendeHendelse.getPayload(), Hendelse.class);
        var aktørId = inngåendeHendelse.getAktørId();
        fordelRestTjeneste.mottaHendelse(aktørId, førsteHendelseInnhold);

        // Alle anses som håndterte etter dette, oppdateres som én gruppe
        var håndtertAv = førsteUhåndterte.getHendelseId();
        alleUhåndterteHendelser.forEach(hendelse ->
            hendelseRepository.oppdaterHåndtertStatus(hendelse, håndtertAv, HåndtertStatusType.HÅNDTERT));
    }

    private InngåendeHendelse hentFørsteInngåendeHendelse(List<InngåendeHendelse> alleUhåndterteHendelser) {
        if (alleUhåndterteHendelser.isEmpty()) {
            throw new IllegalStateException("Skal ha minst én uhåndtert hendelse");
        }
        var førsteUhåndterte = alleUhåndterteHendelser.get(0);
        return førsteUhåndterte;
    }

}
