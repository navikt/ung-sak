package no.nav.k9.kodeverk.vedtak;

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
public enum VedtakResultatType implements Kodeverdi {

    INNVILGET("INNVILGET", "Innvilget"),
    DELVIS_INNVILGET("DELVIS_INNVILGET", "delvis innvilget"),
    AVSLAG("AVSLAG", "Avslag"),
    OPPHØR("OPPHØR", "Opphør"),
    VEDTAK_I_KLAGEBEHANDLING("VEDTAK_I_KLAGEBEHANDLING", "vedtak i klagebehandling"),
    VEDTAK_I_ANKEBEHANDLING("VEDTAK_I_ANKEBEHANDLING", "vedtak i ankebehandling"),
    VEDTAK_I_INNSYNBEHANDLING("VEDTAK_I_INNSYNBEHANDLING", "vedtak i innsynbehandling"),
    UDEFINERT("-", "Ikke definert"),

    ;
    
    private static final Map<String, VedtakResultatType> KODER = new LinkedHashMap<>();
    
    public static final String KODEVERK = "VEDTAK_RESULTAT_TYPE"; //$NON-NLS-1$
    
    @JsonIgnore
    private String navn;
    
    private String kode;

    private VedtakResultatType(String kode) {
        this.kode = kode;
    }

    private VedtakResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static VedtakResultatType fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(VedtakResultatType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VedtakResultatType: for input " + node);
        }
        return ad;
    }
    
    public static Map<String, VedtakResultatType> kodeMap() {
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
}
