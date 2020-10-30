package no.nav.k9.sak.mottak.dokumentmottak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * lagrer inntektsmeldinger til abakus asynk i egen task.
 */
@ApplicationScoped
@ProsessTask(KompletthetskontrollerVurderKompletthetTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class KompletthetskontrollerVurderKompletthetTask extends UnderBehandlingProsessTask {

    static final ObjectMapper OM = new ObjectMapper();

    static {
        OM.registerModule(new JavaTimeModule());
        OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OM.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
    }

    static final String ENDRING_SNAPSHOT = "endring.snapshot";
    static final String TASKTYPE = "kompletthetskontroller.vurder.kompletthet";

    private Kompletthetskontroller kompletthetskontroller;

    KompletthetskontrollerVurderKompletthetTask() {
        // for proxy
    }

    @Inject
    public KompletthetskontrollerVurderKompletthetTask(BehandlingRepository behandlingRepository,
                                                       BehandlingLåsRepository behandlingLåsRepository,
                                                       Kompletthetskontroller kompletthetskontroller) {
        super(behandlingRepository, behandlingLåsRepository);
        this.kompletthetskontroller = kompletthetskontroller;
    }

    @Override
    protected void doProsesser(ProsessTaskData input, Behandling behandling) {

        var behandlingId = behandling.getId();
        var payload = input.getPayloadAsString();

        try {
            var grunnlagSnapshot = OM.readValue(payload, EndringsresultatSnapshot.class);
            kompletthetskontroller.vurderKompletthetOgFortsett(behandling, behandlingId, grunnlagSnapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Kunne ikke deserialisere EndringsresultatSnapshot fra:" + payload, e);
        }
    }

    static ProsessTaskData init(Behandling behandling, EndringsresultatSnapshot grunnlagSnapshot) {
        AktørId aktørId = behandling.getAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();
        var behandlingId = behandling.getId();
        var enkeltTask = new ProsessTaskData(KompletthetskontrollerVurderKompletthetTask.TASKTYPE);
        enkeltTask.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        enkeltTask.setSaksnummer(saksnummer.getVerdi());
        enkeltTask.setCallIdFraEksisterende();
        String payload;
        try {
            payload = OM.writeValueAsString(grunnlagSnapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Kunne ikke serialisere EndringsresultatSnapshot:" + grunnlagSnapshot, e);
        }
        enkeltTask.setPayload(payload);
        return enkeltTask;
    }

}
