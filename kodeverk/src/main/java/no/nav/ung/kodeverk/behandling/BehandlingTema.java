package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

public enum BehandlingTema implements Kodeverdi {
    PLEIEPENGER_SYKT_BARN("PLEIE", "Pleiepenger sykt barn", "ab0320", FagsakYtelseType.PLEIEPENGER_SYKT_BARN), // ny ordning fom 011017
    PLEIEPENGER_LIVETS_SLUTTFASE("PLEIE_PPN", "Pleiepenger i livets sluttfase", "ab0094", FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE),
    OMSORGSPENGER("OMS_OMSORG", "Omsorgspenger", "ab0149", FagsakYtelseType.OMSORGSPENGER),
    UDEFINERT("-", "Ikke definert", null, FagsakYtelseType.UDEFINERT),

    ;

    public static final String KODEVERK = "BEHANDLING_TEMA";
    private static final Map<String, BehandlingTema> KODER = new LinkedHashMap<>();
    private static final Map<String, BehandlingTema> OFFISIELLE_KODER = new LinkedHashMap<>();
    private static final Map<FagsakYtelseType, BehandlingTema> YTELSE_TYPE_TIL_TEMA = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat kode: " + v.kode);
            }
            if (v.offisiellKode != null) {
                OFFISIELLE_KODER.putIfAbsent(v.offisiellKode, v);
            }
            if (YTELSE_TYPE_TIL_TEMA.putIfAbsent(v.fagsakYtelseType, v) != null) {
                throw new IllegalArgumentException("Duplikat ytelseType -> tema: " + v);
            }
        }
    }

    private String navn;

    private String offisiellKode;

    private String kode;

    private FagsakYtelseType fagsakYtelseType;

    private BehandlingTema(String kode) {
        this.kode = kode;
    }

    private BehandlingTema(String kode, String navn, String offisiellKode, FagsakYtelseType fagsakYtelseType) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
        this.fagsakYtelseType = fagsakYtelseType;
    }

    public static BehandlingTema fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingTema: for input " + kode);
        }
        return ad;
    }

    public static BehandlingTema fraOffisiellKode(String kode) {
        if (kode == null) {
            return UDEFINERT;
        }
        return OFFISIELLE_KODER.getOrDefault(kode, UDEFINERT);
    }

    public static Map<String, BehandlingTema> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static BehandlingTema finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

    public static BehandlingTema finnForFagsakYtelseType(FagsakYtelseType ytelseType) {
        return YTELSE_TYPE_TIL_TEMA.getOrDefault(ytelseType, BehandlingTema.UDEFINERT);
    }

    public static BehandlingTema fromString(String kode) {
        return fraKode(kode);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }
}
