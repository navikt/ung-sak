package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum FagsakStatus implements Kodeverdi {

    OPPRETTET("OPPR", "Opprettet"),
    UNDER_BEHANDLING("UBEH", "Under behandling"),
    LØPENDE("LOP", "Løpende"),
    AVSLUTTET("AVSLU", "Avsluttet"),
    ;

    public static final FagsakStatus DEFAULT = OPPRETTET;
    private static final Map<String, FagsakStatus> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FAGSAK_STATUS";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private FagsakStatus(String kode) {
        this.kode = kode;
    }

    private FagsakStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static FagsakStatus fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FagsakStatus: for input " + kode);
        }
        return ad;
    }

    // JsonCreator kompatibilitet for deserialisering frå objekt er beholdt her fordi denne er brukt i sif-abac-pdp
    @JsonCreator
    public static FagsakStatus fraObjektProp(@JsonProperty("kode") final String kode) {
        return fraKode(kode);
    }


    public static Map<String, FagsakStatus> kodeMap() {
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
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

}
