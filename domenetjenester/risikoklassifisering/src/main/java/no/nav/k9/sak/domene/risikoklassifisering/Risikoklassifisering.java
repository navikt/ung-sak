package no.nav.k9.sak.domene.risikoklassifisering;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering.AktoerId;
import no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering.Opplysningsperiode;
import no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering.RequestWrapper;
import no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering.RisikovurderingRequest;
import no.nav.k9.sak.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class Risikoklassifisering {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private ProsessTaskRepository prosessTaskRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private RisikovurderingTjeneste risikovurderingTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    Risikoklassifisering() {
        // CDI proxy
    }

    @Inject
    public Risikoklassifisering(ProsessTaskRepository prosessTaskRepository,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                RisikovurderingTjeneste risikovurderingTjeneste,
                                OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
    }

    public void opprettProsesstaskForRisikovurdering(Behandling behandling) {
        try {
            LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();
            Interval interval = opplysningsPeriodeTjeneste.beregn(behandling.getId(), behandling.getFagsakYtelseType());
            RisikovurderingRequest risikovurderingRequest = RisikovurderingRequest.builder()
                .medSoekerAktoerId(new AktoerId(behandling.getAktørId()))
                .medBehandlingstema(hentBehandlingTema(behandling))
                .medSkjæringstidspunkt(skjæringstidspunkt)
                .medOpplysningsperiode(leggTilOpplysningsperiode(interval))
                .medKonsumentId(behandling.getUuid()).build();
            opprettProsesstask(behandling, risikovurderingRequest);
        } catch (Exception ex) {
            log.warn("Publisering av Risikovurderingstask feilet", ex);
        }
    }

    private String hentBehandlingTema(Behandling behandling) {
        var behandlingTema = behandling.getFagsak().getBehandlingTema();
        return behandlingTema.getOffisiellKode();
    }

    private void opprettProsesstask(Behandling behandling, RisikovurderingRequest risikovurderingRequest) throws IOException {
        if (!risikovurderingTjeneste.behandlingHarBlittRisikoklassifisert(behandling.getId())) {
            ProsessTaskData taskData = new ProsessTaskData(RisikoklassifiseringUtførTask.TASKTYPE);
            taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            taskData.setCallIdFraEksisterende();
            RequestWrapper requestWrapper = new RequestWrapper(MDCOperations.getCallId(), risikovurderingRequest);
            taskData.setProperty(RisikoklassifiseringUtførTask.KONSUMENT_ID, risikovurderingRequest.getKonsumentId().toString());
            taskData.setProperty(RisikoklassifiseringUtførTask.RISIKOKLASSIFISERING_JSON, getJson(requestWrapper));
            prosessTaskRepository.lagre(taskData);
        } else {
            log.info("behandling = {} Har Blitt Risikoklassifisert", behandling.getId());
        }
    }

    private Opplysningsperiode leggTilOpplysningsperiode(Interval interval) {
        LocalDate tilOgMed = interval.getEnd() == null ? null : LocalDate.ofInstant(interval.getEnd(), ZoneId.systemDefault());
        return new Opplysningsperiode(LocalDate.ofInstant(interval.getStart(), ZoneId.systemDefault()), tilOgMed);
    }

    private String getJson(RequestWrapper risikovurderingRequest) throws IOException {
        return JsonObjectMapper.getJson(risikovurderingRequest);
    }
}
