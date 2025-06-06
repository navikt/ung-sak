package no.nav.ung.kodeverk.person;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

public enum Diskresjonskode implements Kodeverdi {
    UDEFINERT("UDEF", "Udefinert", "UDEF"),
    UTENRIKS_TJENST("URIK", "I utenrikstjeneste", "URIK"),
    UTEN_FAST_BO("UFB", "Uten fast bopel", "UFB"),
    SVALBARD("SVAL", "Svalbard", "SVAL"),
    KODE6("SPSF", "Sperret adresse, strengt fortrolig", "SPSF"),
    KODE7("SPFO", "Sperret adresse, fortrolig", "SPFO"),
    PENDLER("PEND", "Pendler", "PEND"),
    MILITÆR("MILI", "Militær", "MILI"),
    KLIENT_ADRESSE("KLIE", "Klientadresse", "KLIE"),
    ;

    private static final String KODEVERK = "DISKRESJONSKODE";
    private static final Map<String, Diskresjonskode> KODER = new LinkedHashMap<>();

    private final String navn;
    private final String offisiellKode;
    private final String kode;

    Diskresjonskode(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static Diskresjonskode fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Diskresjonskode: " + kode);
        }
        return ad;
    }

    public static Map<String, Diskresjonskode> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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
        return getKode();
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    public static Diskresjonskode finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

}
