package no.nav.ung.sak.oppgave;

import jakarta.persistence.*;
import no.nav.ung.sak.felles.BaseEntitet;
import no.nav.ung.sak.felles.typer.AktørId;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "BrukerdialogOppgave")
@Table(name = "BD_OPPGAVE")
public class BrukerdialogOppgaveEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false, updatable = false)))
    private AktørId aktørId;

    @Column(name = "oppgavereferanse", nullable = false, updatable = false, unique = true)
    private UUID oppgavereferanse;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OppgaveStatus status = OppgaveStatus.ULØST;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private OppgaveType oppgaveType;

    @Convert(converter = OppgaveDataConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "data", nullable = false, updatable = false, columnDefinition = "jsonb")
    private OppgaveData data;

    @Column(name = "frist_tid")
    private LocalDateTime fristTid;

    @Column(name = "løst_dato")
    private LocalDateTime løstDato; // NOSONAR

    @Column(name = "åpnet_dato")
    private LocalDateTime åpnetDato; // NOSONAR

    @Column(name = "lukket_dato")
    private LocalDateTime lukketDato; // NOSONAR

    public BrukerdialogOppgaveEntitet(UUID oppgavereferanse,
                                      OppgaveType oppgaveType,
                                      AktørId aktørId,
                                      OppgaveData data,
                                      LocalDateTime fristTid) {
        this.oppgavereferanse = oppgavereferanse;
        this.oppgaveType = oppgaveType;
        this.aktørId = aktørId;
        this.data = data;
        this.fristTid = fristTid;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public UUID getOppgavereferanse() {
        return oppgavereferanse;
    }

    public OppgaveStatus getStatus() {
        return status;
    }

    public OppgaveType getOppgaveType() {
        return oppgaveType;
    }

    public OppgaveData getData() {
        return data;
    }

    public LocalDateTime getFristTid() {
        return fristTid;
    }

    public void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }

    protected void setStatus(OppgaveStatus status) {
        this.status = status;
    }

    public void setLøstDato(LocalDateTime løstDato) {
        this.løstDato = løstDato;
    }

    public LocalDateTime getLøstDato() {
        return løstDato;
    }

    public void setÅpnetDato(LocalDateTime åpnetDato) {
        this.åpnetDato = åpnetDato;
    }

    public LocalDateTime getÅpnetDato() {
        return åpnetDato;
    }

    public void setLukketDato(LocalDateTime lukketDato) {
        this.lukketDato = lukketDato;
    }

    public LocalDateTime getLukketDato() {
        return lukketDato;
    }

    Long getId() {
        return id;
    }
}
