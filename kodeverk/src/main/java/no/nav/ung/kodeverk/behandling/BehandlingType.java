package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public enum BehandlingType implements Kodeverdi {

    /**
     * Konstanter for å skrive ned kodeverdi. For å hente ut andre data konfigurert, må disse leses fra databasen (eks.
     * for å hente offisiell kode for et Nav kodeverk).
     */
    FØRSTEGANGSSØKNAD("BT-002", "Førstegangsbehandling", "ae0034", 6, true),
    REVURDERING("BT-004", "Revurdering", "ae0028", 6, false),

    // Klagebehandlinger og referanser brukt i klageløsning
    KLAGE("BT-003", "Klage", "ae0058", 10, false),
    ANKE("BT-008", "Anke", "ae0046", 10, false),
    TILBAKEKREVING("BT-007", "Tilbakekreving", "BT-007", 10, false),
    REVURDERING_TILBAKEKREVING("BT-009", "Tilbakekreving revurdering", "BT-009", 10, false),

    AKTIVITETSPENGER_DEL_1("BT-011", "Aktivitetspenger del 1", "N/A", 6, true),
    AKTIVITETSPENGER_DEL_2("BT-012", "Aktivitetspenger del 2", "N/A", 6, true),

    UDEFINERT("-", "Ikke definert", null, 0, false),
    ;

    private static final Set<BehandlingType> YTELSE_BEHANDLING_TYPER = Set.of(FØRSTEGANGSSØKNAD, REVURDERING, AKTIVITETSPENGER_DEL_1, AKTIVITETSPENGER_DEL_2);

    public static final String KODEVERK = "BEHANDLING_TYPE";
    private static final Map<String, BehandlingType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }

    }

    private int behandlingstidFristUker;
    private Boolean behandlingstidVarselbrev;

    private String navn;

    private BehandlingType real;

    private String offisiellKode;

    private String kode;

    private BehandlingType(String kode) {
        this.kode = kode;
    }

    private BehandlingType(String kode, String navn, String offisiellKode, int behandlingstidFristUker, Boolean behandlingstidVarselbrev) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
        this.behandlingstidFristUker = behandlingstidFristUker;
        this.behandlingstidVarselbrev = behandlingstidVarselbrev;
    }

    public static BehandlingType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingType: for input " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    public static BehandlingType fromString(String kode) {
        return fraKode(kode);
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public static Set<BehandlingType> getYtelseBehandlingTyper() {
        return YTELSE_BEHANDLING_TYPER;
    }

    public boolean erYtelseBehandlingType() {
        return YTELSE_BEHANDLING_TYPER.contains(this);
    }

    public int getBehandlingstidFristUker() {
        return behandlingstidFristUker;
    }

    public boolean isBehandlingstidVarselbrev() {
        return behandlingstidVarselbrev;
    }
}
