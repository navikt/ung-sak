package no.nav.ung.sak.oppgave.søknad;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveData;

@Entity(name = "BrukerdialogSøknad")
@Table(name = "BD_SOKNAD")
public class BrukerdialogSøknadEntitet extends BrukerdialogOppgaveEntitet {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE")
    protected Long id;

    // Expose setData from parent class
    @Override
    public void setData(OppgaveData data) {
        super.setData(data);
    }
}
