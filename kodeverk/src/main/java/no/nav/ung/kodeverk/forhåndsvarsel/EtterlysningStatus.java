package no.nav.ung.kodeverk.forhåndsvarsel;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.LegacyKodeverdiJsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

@LegacyKodeverdiJsonValue // Serialiserast som kode string i default object mapper
public enum EtterlysningStatus implements Kodeverdi {

    OPPRETTET("OPPRETTET", "Opprettet, ikke sendt ut"),
    VENTER("VENTER", "Sendt og venter på svar"),
    MOTTATT_SVAR("MOTTATT_SVAR", "Mottatt svar"),
    SKAL_AVBRYTES("SKAL_AVBRYTES", "Skal avbrytes"),
    AVBRUTT("AVBRUTT", "Avbrutt"),
    UTLØPT("UTLOPT", "Utløpt"),
    ;

    @JsonValue
    private final String kode;
    private final String navn;


    private static final Map<String, EtterlysningStatus> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


    EtterlysningStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static EtterlysningStatus fraKode(String kode) {
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent EtterlysningType: " + kode);
        }
        return ad;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return "ETTERLYSNING_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }


}
