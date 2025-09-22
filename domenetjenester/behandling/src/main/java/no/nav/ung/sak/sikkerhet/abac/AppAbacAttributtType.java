package no.nav.ung.sak.sikkerhet.abac;

import no.nav.k9.felles.sikkerhet.abac.AbacAttributtType;

/**
 * AbacAttributtTyper som er i bruk for sporingslogg / PDP (Policy Decision Point)
 */
public enum AppAbacAttributtType implements AbacAttributtType {

    DOKUMENT_ID,
    /**
     * egen-definert oppgaveId i Gsak.
     */
    OPPGAVE_ID,
    SAKER_MED_FNR;

    private final boolean maskerOutput;
    ;

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
