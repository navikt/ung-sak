package no.nav.ung.abac;

import no.nav.k9.felles.sikkerhet.abac.AbacAttributtType;

/**
 * AbacAttributtTyper brukes i applikasjonen for å utlede hva som er relevant å sende til PDP for tilgangskontroll
 */
public enum AppAbacAttributtType implements AbacAttributtType {

    DOKUMENT_ID,
    /**
     * egen-definert oppgaveId i Gsak.
     */
    OPPGAVE_ID,
    SAKER_MED_FNR(true);

    private final boolean maskerOutput;

    AppAbacAttributtType() {
        this.maskerOutput = false;
    }

    AppAbacAttributtType(boolean maskerOutput) {
        this.maskerOutput = maskerOutput;
    }

    @Override
    public boolean getMaskerOutput() {
        return maskerOutput;
    }

}
