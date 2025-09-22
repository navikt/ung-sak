package no.nav.ung.abac;

import no.nav.k9.felles.sikkerhet.abac.AbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;

/**
 * AbacAttributtTyper brukes i applikasjonen for å utlede hva som er relevant å sende til PDP for tilgangskontroll
 */
public enum AppAbacAttributtType implements AbacAttributtType {

    DOKUMENT_ID,
    /**
     * egen-definert oppgaveId i Gsak.
     */
    OPPGAVE_ID,
    SAKER_MED_FNR,
    ABAC_ANSVALIG_SAKSBEHANDLER,
    ABAC_BEHANDLING_STATUS,
    ABAC_SAK_STATUS,
    ABAC_AKSJONSPUNKT_TYPE,
    ;

    public static AbacAttributtType AKSJONSPUNKT_KODE = StandardAbacAttributtType.AKSJONSPUNKT_KODE;

    public static AbacAttributtType AKTØR_ID = StandardAbacAttributtType.AKTØR_ID;

    public static AbacAttributtType BEHANDLING_ID = StandardAbacAttributtType.BEHANDLING_ID;

    public static AbacAttributtType BEHANDLING_UUID = StandardAbacAttributtType.BEHANDLING_UUID;

    public static AbacAttributtType FAGSAK_ID = StandardAbacAttributtType.FAGSAK_ID;

    public static AbacAttributtType FNR = StandardAbacAttributtType.FNR;

    public static AbacAttributtType JOURNALPOST_ID = StandardAbacAttributtType.JOURNALPOST_ID;

    public static AbacAttributtType SAKSNUMMER = StandardAbacAttributtType.SAKSNUMMER;

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
