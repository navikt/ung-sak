package no.nav.ung.domenetjenester.personhendelser;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@Dependent
public class ForsinkelseTjeneste {

    private Duration hendelsesforsinkelse;

    @Inject
    public ForsinkelseTjeneste(@KonfigVerdi(value = "INNSENDING_HENDELSER_FORSINKELSE", defaultVerdi = "PT0S") Duration hendelsesforsinkelse) {
        this.hendelsesforsinkelse = hendelsesforsinkelse;
    }

    public LocalDateTime finnTidspunktForInnsendingAvHendelse() {
        return LocalDateTime.now().plus(hendelsesforsinkelse);
    }
}
