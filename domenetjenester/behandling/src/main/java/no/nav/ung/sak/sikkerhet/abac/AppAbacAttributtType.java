package no.nav.ung.sak.sikkerhet.abac;

import no.nav.k9.felles.sikkerhet.abac.AbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;

/**
 * AbacAttributtTyper som er i bruk for sporingslogg / PDP (Policy Decision Point)
 */
public enum AppAbacAttributtType implements AbacAttributtType {

    DOKUMENT_ID("dokumentId"),
    /**
     * egen-definert oppgaveId i Gsak.
     */
    OPPGAVE_ID("oppgaveId"),
    SAKER_MED_FNR("fnrSok", true)
    ;

    private final boolean maskerOutput;
    private final String sporingsloggEksternKode;

    AppAbacAttributtType(String sporingsloggEksternKode) {
        this.sporingsloggEksternKode = sporingsloggEksternKode;
        this.maskerOutput = false;
    }

    AppAbacAttributtType(String sporingsloggEksternKode, boolean maskerOutput) {
        this.sporingsloggEksternKode = sporingsloggEksternKode;
        this.maskerOutput = maskerOutput;
    }

    @Override
    public boolean getMaskerOutput() {
        return maskerOutput;
    }

    @Override
    public String getSporingsloggKode() {
        return sporingsloggEksternKode;
    }
}
