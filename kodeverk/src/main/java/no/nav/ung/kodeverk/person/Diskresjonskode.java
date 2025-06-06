package no.nav.ung.kodeverk.person;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @JsonIgnore
    private final String navn;
    @JsonIgnore
    private final String offisiellKode;
    private final String kode;

    Diskresjonskode(String kode, String navn, String offisiellKode) {
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
    public static Diskresjonskode fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Diskresjonskode.class, node, "kode");
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
