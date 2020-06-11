package no.nav.k9.sak.behandlingslager.fagsak;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "FagsakProsessTask")
@Table(name = "FAGSAK_PROSESS_TASK")
public class FagsakProsessTask extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FAGSAK_PROSESS_TASK")
    private Long id;

    @Column(name = "fagsak_id", nullable = false, updatable = false)
    private Long fagsakId;

    @Column(name = "prosess_task_id", nullable = false, updatable = false)
    private Long prosessTaskId;

    @Column(name = "behandling_id", updatable = false)
    private String behandlingId;

    @Column(name = "gruppe_sekvensnr", updatable = false)
    private Long gruppeSekvensNr;

    /** duplisert fra ProsessTask for å kunne ha db constraints på type her (uten å sette inn kompositt-nøkler). */
    @Column(name = "task_type", updatable = false)
    private String taskType;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    FagsakProsessTask() {
        // Hibernate trenger en
    }

    public FagsakProsessTask(Long fagsakId, String behandlingId, Long prosessTaskId, Long gruppeSekvensNr, String taskType) {
        this.fagsakId = fagsakId;
        this.prosessTaskId = prosessTaskId;
        this.behandlingId = behandlingId;
        this.gruppeSekvensNr = gruppeSekvensNr;
        this.taskType = Objects.requireNonNull(taskType, "taskType");
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getProsessTaskId() {
        return prosessTaskId;
    }

    public Long getGruppeSekvensNr() {
        return gruppeSekvensNr;
    }

    public String getTaskType() {
        return taskType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !(obj instanceof FagsakProsessTask)) {
            return false;
        }
        FagsakProsessTask other = (FagsakProsessTask) obj;
        return Objects.equals(prosessTaskId, other.prosessTaskId)
            && Objects.equals(fagsakId, other.fagsakId)
            && Objects.equals(taskType, other.taskType)
            && Objects.equals(behandlingId, other.behandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prosessTaskId, fagsakId, behandlingId, taskType);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "prosessTask=" + prosessTaskId
            + ", fagsak=" + fagsakId
            + (behandlingId == null ? "" : ", behandling=" + behandlingId)
            + (taskType == null ? "" : ", taskType=" + taskType)
            + (gruppeSekvensNr == null ? "" : ", gruppeSekvensNr=" + gruppeSekvensNr)
            + ">";
    }
}
