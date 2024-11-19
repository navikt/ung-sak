package no.nav.ung.kodeverk.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingType implements Kodeverdi {

    /**
     * Konstanter for å skrive ned kodeverdi. For å hente ut andre data konfigurert, må disse leses fra databasen (eks.
     * for å hente offisiell kode for et Nav kodeverk).
     */
    FØRSTEGANGSSØKNAD("BT-002", "Førstegangsbehandling", "ae0034", 6, true),
    REVURDERING("BT-004", "Revurdering", "ae0028", 6, false),
    UNNTAKSBEHANDLING("BT-010", "Unntaksbehandling", "N/A", 6, false),
    UDEFINERT("-", "Ikke definert", null, 0, false),
    ;

    private static final Set<BehandlingType> YTELSE_BEHANDLING_TYPER = Set.of(FØRSTEGANGSSØKNAD, REVURDERING, UNNTAKSBEHANDLING);

    public static final String KODEVERK = "BEHANDLING_TYPE";
    private static final Map<String, BehandlingType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }

    }

    @JsonIgnore
    private int behandlingstidFristUker;
    @JsonIgnore
    private Boolean behandlingstidVarselbrev;

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private BehandlingType real;

    @JsonIgnore
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

    /**
     * toString is set to output the kode value of the enum instead of the default that is the enum name.
     * This makes the generated openapi spec correct when the enum is used as a query param. Without this the generated
     * spec incorrectly specifies that it is the enum name string that should be used as input.
     */
    @Override
    public String toString() {
        return this.getKode();
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static BehandlingType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BehandlingType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingType: for input " + node);
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

    @JsonProperty(value = "kode")
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty(value = "kodeverk", access = JsonProperty.Access.READ_ONLY)
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
