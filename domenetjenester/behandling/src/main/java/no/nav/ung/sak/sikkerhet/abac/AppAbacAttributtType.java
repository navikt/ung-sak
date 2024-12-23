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
    SAKER_MED_FNR("fnrSok"),
    ABAC_ANSVALIG_SAKSBEHANDLER("ansvarlig_saksbehandler"),
    ABAC_BEHANDLING_STATUS("behandling_status"),
    ABAC_SAK_STATUS("sak_status"),
    ABAC_AKSJONSPUNKT_TYPE("aksjonspunkt_type"),
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
