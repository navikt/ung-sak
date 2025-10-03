package no.nav.ung.sak.hendelsemottak.tjenester.kabal.kontrakt;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *  Se <a href="https://github.com/navikt/kabal-api/blob/main/docs/schema/behandling-events.json">kabal-api schema</a>
 */
public record KabalBehandlingEvent(
    UUID behandlingUuid,
    UUID eventId,
    String kildeReferanse,
    String kilde,
    String kabalReferanse,
    /*
     * KLAGEBEHANDLING_AVSLUTTET
     * ANKEBEHANDLING_OPPRETTET
     * ANKEBEHANDLING_AVSLUTTET
     * BEHANDLING_FEILREGISTRERT
     */
    String type,
    BehandlingDetaljer detaljer
) {

    public record BehandlingDetaljer(
        KlagebehandlingAvsluttetDetaljer klagebehandlingAvsluttet,
        AnkebehandlingOpprettetDetaljer ankebehandlingOpprettet,
        AnkebehandlingAvsluttetDetaljer ankebehandlingAvsluttet,
        BehandlingFeilregistrertDetaljer behandlingFeilregistrert
    ) {
    }

    public record KlagebehandlingAvsluttetDetaljer(
        LocalDateTime avsluttet,
        Utfall utfall,
        List<String> journalpostReferanser
    ) {
    }

    public record AnkebehandlingOpprettetDetaljer(
        LocalDateTime mottattKlageinstans
    ) {
    }

    public record AnkebehandlingAvsluttetDetaljer(
        LocalDateTime avsluttet,
        Utfall utfall,
        List<String> journalpostReferanser
    ) {
    }

    public record BehandlingFeilregistrertDetaljer(
        LocalDateTime feilregistrert,
        String navIdent,
        String reason,
        String type
    ){}


    public enum Utfall {
        TRUKKET("TRUKKET"),
        RETUR("RETUR"),
        OPPHEVET("OPPHEVET"),
        MEDHOLD("MEDHOLD"),
        DELVIS_MEDHOLD("DELVIS_MEDHOLD"),
        STADFESTELSE("STADFESTELSE"),
        UGUNST("UGUNST"),
        AVVIST("AVVIST");
        private final String value;
        private final static Map<String, Utfall> CONSTANTS = new HashMap<String, Utfall>();

        static {
            for (Utfall c : values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Utfall(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static Utfall fromValue(String value) {
            Utfall constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    //Bruker string istedenfor for å unnge å feil ved parsing.
    public static class Eventtyper {
        public static final String KLAGEBEHANDLING_AVSLUTTET = "KLAGEBEHANDLING_AVSLUTTET";
        public static final String ANKEBEHANDLING_OPPRETTET = "ANKEBEHANDLING_OPPRETTET";
        public static final String ANKEBEHANDLING_AVSLUTTET = "ANKEBEHANDLING_AVSLUTTET";
        public static final String BEHANDLING_FEILREGISTRERT = "BEHANDLING_FEILREGISTRERT";
    }
}
