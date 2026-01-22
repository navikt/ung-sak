package no.nav.ung.sak.oppgave.søknad;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveData;

@Entity(name = "BrukerdialogSøknad")
@Table(name = "BD_SOKNAD")
public class BrukerdialogSøknadEntitet extends BrukerdialogOppgaveEntitet {

    // Expose setData from parent class
    @Override
    public void setData(OppgaveData data) {
        super.setData(data);
    }
}
