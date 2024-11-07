package no.nav.k9.kodeverk.dokument;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Kommunikasjonsretning {
    /**
     * Inngående dokument
     */
    INN("I"),
    /**
     * Utgående dokument
     */
    UT("U"),
    /**
     * Internt notat
     */
    NOTAT("N");

    private static final Map<String, Kommunikasjonsretning> KODER;

    @JsonValue
    private String kode;

    static {
        Map<String, Kommunikasjonsretning> map = new ConcurrentHashMap<>();
        for (Kommunikasjonsretning kommunikasjonsretning : Kommunikasjonsretning.values()) {
            map.put(kommunikasjonsretning.getKode(), kommunikasjonsretning);
        }
        KODER = Collections.unmodifiableMap(map);
    }

    Kommunikasjonsretning(String kode) {
        this.kode = kode;
    }

    @JsonCreator
    public static Kommunikasjonsretning fromKommunikasjonsretningCode(String kode) {
        return KODER.get(kode);
    }

    public String getKode() {
        return kode;
    }
}
