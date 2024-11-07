package no.nav.k9.kodeverk.dokument;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum DokumentGruppe implements Kodeverdi {

    SØKNAD("SØKNAD", "Søknad"),
    INNTEKTSMELDING("INNTEKTSMELDING", "Inntektsmelding"),
    VEDLEGG("VEDLEGG", "Vedlegg"),
    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "DOKUMENT_GRUPPE";

    private static final Map<String, DokumentGruppe> KODER = new LinkedHashMap<>();

    @JsonIgnore
    private String navn;

    private String kode;

    private DokumentGruppe(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static DokumentGruppe  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(DokumentGruppe.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent DokumentGruppe: " + kode);
        }
        return ad;
    }

    public static Map<String, DokumentGruppe> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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
        return getKode();
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}
