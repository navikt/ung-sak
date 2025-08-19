package no.nav.ung.sak.web.server.abac;

import java.util.Set;


public class UkjentFagsakException extends UkjentAbacVerdiException {

    protected UkjentFagsakException(Set<Long> fagsakId) {
        super("Minst en av fagsakene med id " + fagsakId + " finnes ikke i applikasjonen.");
    }
}
