package no.nav.k9.sak.hendelsemottak.k9fordel.kafka;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.kodeverk.hendelser.HåndtertStatusType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.hendelsemottak.k9fordel.domene.HendelseRepository;
import no.nav.k9.sak.hendelsemottak.k9fordel.domene.InngåendeHendelse;
import no.nav.k9.sak.hendelsemottak.k9fordel.task.SendInnHendelseTask;
import no.nav.k9.sak.hendelsemottak.resttjenester.FordelHendelseRestTjeneste;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.kontrakt.hendelser.JsonMapper;
import no.nav.k9.sak.typer.AktørId;
import no.nav.person.pdl.leesah.Personhendelse;


@Transactional
@ActivateRequestContext
@ApplicationScoped
public class PdlLeesahHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(PdlLeesahHendelseHåndterer.class);

    private PdlLeesahOversetter oversetter;
    private FordelHendelseRestTjeneste fordelRestTjeneste;
    private HendelseRepository hendelseRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private ForsinkelseTjeneste forsinkelseTjeneste;

    public PdlLeesahHendelseHåndterer() {
        // CDI
    }

    @Inject
    public PdlLeesahHendelseHåndterer(PdlLeesahOversetter oversetter, FordelHendelseRestTjeneste fordelHendelseRestTjeneste, HendelseRepository hendelseRepository, ProsessTaskRepository prosessTaskRepository, ForsinkelseTjeneste forsinkelseTjeneste) {
        this.oversetter = oversetter;
        this.fordelRestTjeneste = fordelHendelseRestTjeneste;
        this.hendelseRepository = hendelseRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.forsinkelseTjeneste = forsinkelseTjeneste;
    }


    void handleMessage(Personhendelse payload) {
        setCallIdForHendelse(payload);

        Optional<Hendelse> oversattHendelse = oversetter.oversettPersonhendelse(payload);
        if (oversattHendelse.isPresent()) {
            håndterPersonhendelse(oversattHendelse.get());
        } else {
            LOG.info("Mottok en hendelse fra PDL som ignoreres: hendelseId={} opplysningstype={} endringstype={} master={} opprettet={} tidligereHendelseId={}",
                payload.getHendelseId(), payload.getOpplysningstype(), payload.getEndringstype(), payload.getMaster(), payload.getOpprettet(), payload.getTidligereHendelseId());
        }
    }

    private void håndterPersonhendelse(Hendelse hendelse) {
        var hendelseInfo = hendelse.getHendelseInfo();

        var aktørIder = hendelseInfo.getAktørIder();
        if (aktørIder.size() > 1) {
            LOG.info("Personhendelse tilsvarer én person, men hadde flere aktørId-er, antall={}", aktørIder.size());
        }

        for (AktørId aktørId : aktørIder) {
            var fagsaker = fordelRestTjeneste.finnPåvirkedeFagsaker(aktørId, hendelse);
            if(!fagsaker.isEmpty()) {
                LOG.info("Relevant hendelseId={} av hendelseType={} lagres for videre prosessering", hendelseInfo.getHendelseId(), hendelse.getHendelseType());
                InngåendeHendelse inngåendeHendelse = lagreInngåendeHendelse(aktørId, hendelse, HåndtertStatusType.MOTTATT);
                opprettSendInnHendelseTask(inngåendeHendelse.getId());
            } else {
                LOG.info("Ikke-relevant hendelseId={} av hendelseType={} filtrert bort", hendelseInfo.getHendelseId(), hendelse.getHendelseType());
            }
        }
    }


    private InngåendeHendelse lagreInngåendeHendelse(AktørId aktørId, Hendelse hendelse, HåndtertStatusType håndtertStatusType) {
        var hendelseInfo = hendelse.getHendelseInfo();
        InngåendeHendelse inngåendeHendelse = InngåendeHendelse.builder()
            .aktørId(aktørId)
            .hendelseType(hendelse.getHendelseType())
            .hendelseId(hendelseInfo.getHendelseId())
            .meldingOpprettet(hendelseInfo.getOpprettet())
            .hendelseKilde(hendelseInfo.getHendelseKilde())
            .payload(JsonMapper.toJson(hendelse))
            .håndtertStatus(håndtertStatusType)
            .build();
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        return inngåendeHendelse;
    }

    private void opprettSendInnHendelseTask(Long inngåendeHendelseId) {
        var prosessTaskData = new ProsessTaskData(SendInnHendelseTask.TASKNAME);
        prosessTaskData.setProperty(SendInnHendelseTask.INNGÅENDE_HENDELSE_ID, "" + inngåendeHendelseId);

        LocalDateTime tidspunktForInnsendingAvHendelse = forsinkelseTjeneste.finnTidspunktForInnsendingAvHendelse();
        prosessTaskData.setNesteKjøringEtter(tidspunktForInnsendingAvHendelse);

        prosessTaskRepository.lagre(prosessTaskData);
    }


    private void setCallIdForHendelse(Personhendelse payload) {
        var hendelsesId = payload.getHendelseId();
        if (hendelsesId == null || hendelsesId.toString().isEmpty()) {
            MDCOperations.putCallId(UUID.randomUUID().toString());
        } else {
            MDCOperations.putCallId(hendelsesId.toString());
        }
    }

}
