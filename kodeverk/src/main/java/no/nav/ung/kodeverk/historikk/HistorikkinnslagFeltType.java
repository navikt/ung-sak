package no.nav.ung.kodeverk.historikk;

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
import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum HistorikkinnslagFeltType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    AARSAK("AARSAK", "aarsak"),
    BEGRUNNELSE("BEGRUNNELSE", "begrunnelse"),
    HENDELSE("HENDELSE", "hendelse"),
    RESULTAT("RESULTAT", "resultat"),
    OPPLYSNINGER("OPPLYSNINGER", "opplysninger"),
    ENDRET_FELT("ENDRET_FELT", "endredeFelter"),
    SKJERMLENKE("SKJERMLENKE", "skjermlenke"),
    GJELDENDE_FRA("GJELDENDE_FRA", "Gjeldende fra"),
    AKSJONSPUNKT_BEGRUNNELSE("AKSJONSPUNKT_BEGRUNNELSE", "aksjonspunktBegrunnelse"),
    AKSJONSPUNKT_GODKJENT("AKSJONSPUNKT_GODKJENT", "aksjonspunktGodkjent"),
    AKSJONSPUNKT_KODE("AKSJONSPUNKT_KODE", "aksjonspunktKode"),
    AVKLART_SOEKNADSPERIODE("AVKLART_SOEKNADSPERIODE", "Avklart soeknadsperiode"),
    ANGÅR_TEMA("ANGÅR_TEMA", "Angår tema"),
    ;

    private static final Map<String, HistorikkinnslagFeltType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKKINNSLAG_FELT_TYPE";

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

    private HistorikkinnslagFeltType(String kode) {
        this.kode = kode;
    }

    private HistorikkinnslagFeltType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static HistorikkinnslagFeltType  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(HistorikkinnslagFeltType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkinnslagFeltType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkinnslagFeltType> kodeMap() {
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
