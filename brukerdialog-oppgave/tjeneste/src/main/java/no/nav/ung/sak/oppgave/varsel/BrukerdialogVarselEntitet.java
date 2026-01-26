package no.nav.ung.sak.oppgave.varsel;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveData;

import java.time.LocalDateTime;

@Entity(name = "BrukerdialogVarsel")
@Table(name = "BD_VARSEL")
public class BrukerdialogVarselEntitet extends BrukerdialogOppgaveEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE")
    protected Long id;

    @Column(name = "frist_tid")
    private LocalDateTime fristTid;

    public LocalDateTime getFristTid() {
        return fristTid;
    }

    public void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }

    // Expose setData from parent class
    @Override
    public void setData(OppgaveData data) {
        super.setData(data);
    }
}
