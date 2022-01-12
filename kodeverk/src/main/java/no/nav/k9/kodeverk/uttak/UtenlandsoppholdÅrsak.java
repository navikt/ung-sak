package no.nav.k9.kodeverk.uttak;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum UtenlandsoppholdÅrsak implements Kodeverdi {
    BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING(
        "BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING",
        "Barnet innlagt i helseinstitusjon for norsk offentlig regning"),
    BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD(
        "BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD",
        "Barnet innlagt i helseinstitusjon dekket etter avtale med et annet land om trygd");

    private final String kode;
    private final String navn;

    public static final String KODEVERK = "UTENLANDSOPPHOLD_ÅRSAK";

    private static final Map<String, UtenlandsoppholdÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            KODER.putIfAbsent(v.name(), v); // fallback for Jackson enum key i map issue (løses delvis i jackson 2.11)
        }
    }

    private UtenlandsoppholdÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static UtenlandsoppholdÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UtenlandsoppholdÅrsak: " + kode);
        }
        return ad;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return this.getKode();
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }
}
