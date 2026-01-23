package no.nav.ung.sak.oppgave;

import jakarta.persistence.*;
import no.nav.ung.sak.felles.BaseEntitet;
import no.nav.ung.sak.felles.typer.AktørId;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@MappedSuperclass
public abstract class BrukerdialogOppgaveEntitet extends BaseEntitet {

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", unique = true, nullable = false, updatable = false)))
    protected AktørId aktørId;

    @Column(name = "oppgavereferanse", nullable = false, updatable = false, unique = true)
    private UUID oppgavereferanse;

    @Column(name = "status", nullable = false, updatable = false)
    protected OppgaveStatus status;

    @Column(name = "type")
    protected OppgaveType oppgaveType;

    @Convert(converter = OppgaveDataConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "data", nullable = false, updatable = false, columnDefinition = "jsonb")
    private OppgaveData data;

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

    protected void setData(OppgaveData data) {
        this.data = data;
    }

}
