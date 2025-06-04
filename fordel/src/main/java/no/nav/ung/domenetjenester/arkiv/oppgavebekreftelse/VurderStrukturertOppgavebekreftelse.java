package no.nav.ung.domenetjenester.arkiv.oppgavebekreftelse;

import no.nav.ung.domenetjenester.sak.FinnEllerOpprettUngSakTask;
import no.nav.ung.fordel.handler.MottattMelding;

public class VurderStrukturertOppgavebekreftelse {

    public MottattMelding håndtertOppgavebekreftelse(MottattMelding dataWrapper) {
        return dataWrapper.nesteSteg(FinnEllerOpprettUngSakTask.TASKTYPE);
    }

}
