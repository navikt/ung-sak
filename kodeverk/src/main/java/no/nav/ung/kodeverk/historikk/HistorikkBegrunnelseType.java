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
public enum HistorikkBegrunnelseType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    SAKSBEH_START_PA_NYTT("SAKSBEH_START_PA_NYTT", "Saksbehandling starter på nytt"),
    BEH_STARTET_PA_NYTT("BEH_STARTET_PA_NYTT", "Behandling startet på nytt"),
    BERORT_BEH_ENDRING_DEKNINGSGRAD("BERORT_BEH_ENDRING_DEKNINGSGRAD", "Endring i den andre forelderens dekningsgrad"),
    BERORT_BEH_OPPHOR("BERORT_BEH_OPPHOR", "Den andre forelderens vedtak er opphørt"),
    ;

    private static final Map<String, HistorikkBegrunnelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_BEGRUNNELSE_TYPE";

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

    private HistorikkBegrunnelseType(String kode) {
        this.kode = kode;
    }

    private HistorikkBegrunnelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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
    public static HistorikkBegrunnelseType  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(HistorikkBegrunnelseType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkBegrunnelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkBegrunnelseType> kodeMap() {
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
