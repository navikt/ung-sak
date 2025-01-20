package no.nav.ung.domenetjenester.personhendelser;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.ung.domenetjenester.personhendelser.utils.PersonhendelseUtils;
import no.nav.ung.fordel.repo.hendelser.HendelseRepository;
import no.nav.ung.fordel.repo.hendelser.InngåendeHendelseEntitet;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
@ProsessTask(HåndterUngSakHendelseTask.TASKNAME)
public class HåndterUngSakHendelseTask implements ProsessTaskHandler {

    public static final String TASKNAME = "ung.hendelseHåndterer";
    private static final Logger LOG = LoggerFactory.getLogger(HåndterUngSakHendelseTask.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private HendelseRepository hendelseRepository;
    private ForsinkelseTjeneste forsinkelseTjeneste;
    private PdlLeesahHendelseFiltrerer hendelseFiltrerer;
    private PdlLeesahOversetter pdlLeesahOversetter;

    public HåndterUngSakHendelseTask() {
        // CDI
    }

    @Inject
    public HåndterUngSakHendelseTask(ProsessTaskTjeneste prosessTaskTjeneste,
                                     HendelseRepository hendelseRepository,
                                     ForsinkelseTjeneste forsinkelseTjeneste,
                                     PdlLeesahHendelseFiltrerer hendelseFiltrerer, PdlLeesahOversetter pdlLeesahOversetter) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.hendelseRepository = hendelseRepository;
        this.forsinkelseTjeneste = forsinkelseTjeneste;
        this.hendelseFiltrerer = hendelseFiltrerer;
        this.pdlLeesahOversetter = pdlLeesahOversetter;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final String payload = prosessTaskData.getPayloadAsString();
        Personhendelse personhendelse = PersonhendelseUtils.fraJson(payload);
        final Hendelse oversattHendelse = pdlLeesahOversetter.oversettStøttetPersonhendelse(personhendelse).orElseThrow();

        LOG.info("Håndterer hendelseId={}", oversattHendelse.getHendelseInfo().getHendelseId());

        final List<AktørId> aktørerMedPåvirketFagsak = hendelseFiltrerer.finnAktørerMedPåvirketUngFagsak(oversattHendelse);

        if (aktørerMedPåvirketFagsak.size() > 1) {
            LOG.warn("Personhendelse tilsvarer én person, men hadde flere aktørId-er med påvirket sak, hendelseId={} antall={}", oversattHendelse.getHendelseInfo().getHendelseId(), aktørerMedPåvirketFagsak.size());
        }
        if (aktørerMedPåvirketFagsak.isEmpty()) {
            LOG.warn("Forventet treff på påvirkede saker ved prefiltrering. Avbryter videre håndtering av hendelseId={}", oversattHendelse.getHendelseInfo().getHendelseId());
            return;
        }

        for (AktørId aktørId : aktørerMedPåvirketFagsak) {
            LOG.info("Relevant hendelseId={} av hendelseType={} lagres for videre prosessering", oversattHendelse.getHendelseInfo().getHendelseId(), oversattHendelse.getHendelseType());
            InngåendeHendelseEntitet inngåendeHendelse = lagreInngåendeHendelse(aktørId, oversattHendelse, InngåendeHendelseEntitet.HåndtertStatusType.MOTTATT);
            opprettSendInnHendelseTask(inngåendeHendelse.getId());
        }
    }

    private InngåendeHendelseEntitet lagreInngåendeHendelse(AktørId aktørId, Hendelse hendelse, InngåendeHendelseEntitet.HåndtertStatusType håndtertStatusType) {
        final HendelseInfo hendelseInfo = hendelse.getHendelseInfo();
        final String payload = HendelseMapper.toJson(hendelse);
        InngåendeHendelseEntitet inngåendeHendelse = InngåendeHendelseEntitet.builder()
            .aktørId(aktørId.getAktørId())
            .hendelseType(hendelse.getHendelseType())
            .hendelseId(hendelseInfo.getHendelseId())
            .meldingOpprettet(hendelseInfo.getOpprettet())
            .payload(payload)
            .håndtertStatus(håndtertStatusType)
            .build();
        hendelseRepository.lagreInngåendeHendelse(inngåendeHendelse);
        return inngåendeHendelse;
    }

    private void opprettSendInnHendelseTask(Long inngåendeHendelseId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(SendInnUngHendelseTask.class);
        prosessTaskData.setProperty(SendInnUngHendelseTask.INNGÅENDE_HENDELSE_ID, "" + inngåendeHendelseId);

        LocalDateTime tidspunktForInnsendingAvHendelse = forsinkelseTjeneste.finnTidspunktForInnsendingAvHendelse();
        prosessTaskData.setNesteKjøringEtter(tidspunktForInnsendingAvHendelse);

        prosessTaskTjeneste.lagre(prosessTaskData);
    }


}
