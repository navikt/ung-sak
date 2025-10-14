package no.nav.ung.sak.web.server.abac;

import java.util.Collection;


public class UkjentFagsakException extends UkjentAbacVerdiException {

    protected UkjentFagsakException(Collection<String> saksnummer) {
        super("Minst en av fagsakene med id " + saksnummer + " finnes ikke i applikasjonen.");
    }
}
