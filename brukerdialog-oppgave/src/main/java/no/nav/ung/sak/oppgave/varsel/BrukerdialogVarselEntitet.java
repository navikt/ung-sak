package no.nav.ung.sak.oppgave.varsel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveData;

import java.time.LocalDateTime;

@Entity(name = "BrukerdialogVarsel")
@Table(name = "BD_VARSEL")
public class BrukerdialogVarselEntitet extends BrukerdialogOppgaveEntitet {

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
