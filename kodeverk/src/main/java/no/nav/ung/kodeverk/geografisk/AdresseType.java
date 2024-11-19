package no.nav.ung.kodeverk.geografisk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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
public enum AdresseType implements Kodeverdi {

    BOSTEDSADRESSE("BOSTEDSADRESSE", "Bostedsadresse", "BOAD"),
    DELT_BOSTEDSADRESSE("DELT_BOSTEDSADRESSE", "Delt Bostedsadresse", null),
    POSTADRESSE("POSTADRESSE", "Postadresse", "POST"),
    POSTADRESSE_UTLAND("POSTADRESSE_UTLAND", "Postadresse i utlandet", "PUTL"),
    MIDLERTIDIG_POSTADRESSE_NORGE("MIDLERTIDIG_POSTADRESSE_NORGE", "Midlertidig postadresse i Norge", "TIAD"),
    MIDLERTIDIG_POSTADRESSE_UTLAND("MIDLERTIDIG_POSTADRESSE_UTLAND", "Midlertidig postadresse i utlandet", "UTAD"),
    UKJENT_ADRESSE("UKJENT_ADRESSE", "Ukjent adresse", "UKJE"),
    ;

    private static final Map<String, AdresseType> KODER = new LinkedHashMap<>();

    private static final String KODEVERK = "ADRESSE_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;
    @JsonIgnore String offisiellKode;

    private AdresseType(String kode) {
        this.kode = kode;
    }

    private AdresseType(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
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
    public static AdresseType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(AdresseType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AdresseType: " + kode);
        }
        return ad;
    }

    public static Map<String, AdresseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static Optional<AdresseType> fraKodeOptional(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(KODER.get(kode));
    }


    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
