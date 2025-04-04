package no.nav.ung.sak.web.app.tjenester;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.ung.sak.kontrakt.AsyncPollingStatus;

public class VurderProsessTaskStatusForPollingApi {
    private static final Logger log = LoggerFactory.getLogger(VurderProsessTaskStatusForPollingApi.class);
    private static final Set<ProsessTaskStatus> FERDIG_STATUSER = Set.of(ProsessTaskStatus.FERDIG, ProsessTaskStatus.KJOERT);
    private ProsessTaskFeilmelder feilmelder;
    private Long entityId;

    public VurderProsessTaskStatusForPollingApi(ProsessTaskFeilmelder feilmelder, Long entityId) {
        this.feilmelder = feilmelder;
        this.entityId = entityId;
    }

    public Optional<AsyncPollingStatus> sjekkStatusNesteProsessTask(String gruppe, Map<String, ProsessTaskData> nesteTask) {
        LocalDateTime maksTidFørNesteKjøring = LocalDateTime.now().plusMinutes(2);
        nesteTask = nesteTask.entrySet().stream()
            .filter(e -> !FERDIG_STATUSER.contains(e.getValue().getStatus())) // trenger ikke FERDIG
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!nesteTask.isEmpty()) {
            Optional<ProsessTaskData> optTask = Optional.ofNullable(nesteTask.get(gruppe));
            if (optTask.isEmpty()) {
                // plukker neste til å polle på
                optTask = nesteTask.entrySet().stream()
                    .map(e -> e.getValue())
                    .findFirst();
            }

            if (optTask.isPresent()) {
                return sjekkStatus(maksTidFørNesteKjøring, optTask);
            }
        }
        return Optional.empty();
    }

    private Optional<AsyncPollingStatus> sjekkStatus(LocalDateTime maksTidFørNesteKjøring, Optional<ProsessTaskData> optTask) {
        ProsessTaskData task = optTask.get();
        String gruppe = task.getGruppe();
        String callId = task.getPropertyValue("callId");
        ProsessTaskStatus taskStatus = task.getStatus();
        if (ProsessTaskStatus.KLAR.equals(taskStatus) || ProsessTaskStatus.VETO.equals(taskStatus)) {
            return ventPåKlar(gruppe, maksTidFørNesteKjøring, task, callId);
        } else if (ProsessTaskStatus.VENTER_SVAR.equals(taskStatus)) {
            return ventPåSvar(gruppe, task, callId);
        } else {
            // dekker SUSPENDERT, FEILET
            return håndterFeil(gruppe, task, callId);
        }
    }

    private Optional<AsyncPollingStatus> håndterFeil(String gruppe, ProsessTaskData task, String callId) {
        Feil feil = feilmelder.feilIProsessTaskGruppe(callId, entityId, gruppe, task.getTaskType(), task.getId(), task.getStatus(), task.getSistKjørt());
        feil.log(log);

        AsyncPollingStatus status = new AsyncPollingStatus(AsyncPollingStatus.Status.HALTED,
            null, task.getSisteFeil());
        return Optional.of(status);// fortsett å polle på gruppe, er ikke ferdig.
    }

    private Optional<AsyncPollingStatus> ventPåSvar(String gruppe, ProsessTaskData task, String callId) {
        if (task.getSistKjørt() != null && LocalDateTime.now().isBefore(task.getSistKjørt().plusMinutes(2))) {
            AsyncPollingStatus status = new AsyncPollingStatus(
                AsyncPollingStatus.Status.PENDING,
                task.getNesteKjøringEtter(),
                "Venter på svar for prosesstask [" + task.getTaskType() + "][id: " + task.getId() + "]",
                null, 500L);

            return Optional.of(status);// fortsett å polle på gruppe, er ikke ferdig.
        } else {
            Feil feil = feilmelder.venterPåSvar(callId, entityId, gruppe, task.getTaskType(), task.getId(), task.getStatus(), task.getSistKjørt());
            feil.log(log);

            AsyncPollingStatus status = new AsyncPollingStatus(
                AsyncPollingStatus.Status.DELAYED,
                task.getNesteKjøringEtter(),
                feil.getFeilmelding());

            return Optional.of(status);// er ikke ferdig, men ok å videresende til visning av behandling med feilmelding der.
        }
    }

    private Optional<AsyncPollingStatus> ventPåKlar(String gruppe, LocalDateTime maksTidFørNesteKjøring, ProsessTaskData task, String callId) {
        if (task.getNesteKjøringEtter() == null || task.getNesteKjøringEtter().isBefore(maksTidFørNesteKjøring)) {

            AsyncPollingStatus status = new AsyncPollingStatus(
                AsyncPollingStatus.Status.PENDING,
                task.getNesteKjøringEtter(),
                "Venter på prosesstask [" + task.getTaskType() + "][id: " + task.getId() + "]",
                null, 500L);

            return Optional.of(status);// fortsett å polle på gruppe, er ikke ferdig.
        } else {
            Feil feil = feilmelder.utsattKjøringAvProsessTask(
                callId, entityId, gruppe, task.getTaskType(), task.getId(), task.getStatus(), task.getNesteKjøringEtter());
            feil.log(log);

            AsyncPollingStatus status = new AsyncPollingStatus(
                AsyncPollingStatus.Status.DELAYED,
                task.getNesteKjøringEtter(),
                feil.getFeilmelding());

            return Optional.of(status);// er ikke ferdig, men ok å videresende til visning av behandling med feilmelding der.
        }
    }

    public interface ProsessTaskFeilmelder {
        Feil feilIProsessTaskGruppe(String callId, Long entityId, String gruppe, String taskType, Long taskId, ProsessTaskStatus taskStatus, LocalDateTime sistKjørt);

        Feil utsattKjøringAvProsessTask(String callId, Long entityId, String gruppe, String taskType, Long taskId, ProsessTaskStatus taskStatus, LocalDateTime nesteKjøringEtter);

        Feil venterPåSvar(String callId, Long entityId, String gruppe, String taskType, Long id, ProsessTaskStatus status, LocalDateTime sistKjørt);
    }

}
