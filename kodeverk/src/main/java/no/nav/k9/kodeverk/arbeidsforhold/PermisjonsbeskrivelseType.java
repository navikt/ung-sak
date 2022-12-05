package no.nav.k9.kodeverk.arbeidsforhold;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
public enum PermisjonsbeskrivelseType implements Kodeverdi {

    UDEFINERT("-", "Ikke definert", null),
    PERMISJON("PERMISJON", "Permisjon", "permisjon"),
    UTDANNINGSPERMISJON("UTDANNINGSPERMISJON", "Utdanningspermisjon", "utdanningspermisjon"), // Utgår 31/12-2022
    UTDANNINGSPERMISJON_IKKE_LOVFESTET("UTDANNINGSPERMISJON_IKKE_LOVFESTET", "Utdanningspermisjon (Ikke lovfestet)", "utdanningspermisjonIkkeLovfestet"),
    UTDANNINGSPERMISJON_LOVFESTET("UTDANNINGSPERMISJON_LOVFESTET", "Utdanningspermisjon (Lovfestet)", "utdanningspermisjonLovfestet"),
    VELFERDSPERMISJON("VELFERDSPERMISJON", "Velferdspermisjon", "velferdspermisjon"), // Utgår 31/12-2022
    ANNEN_PERMISJON_IKKE_LOVFESTET("ANNEN_PERMISJON_IKKE_LOVFESTET", "Andre ikke-lovfestede permisjoner", "andreIkkeLovfestedePermisjoner"),
    ANNEN_PERMISJON_LOVFESTET("ANNEN_PERMISJON_LOVFESTET", "Andre lovfestede permisjoner", "andreLovfestedePermisjoner"),
    PERMISJON_MED_FORELDREPENGER("PERMISJON_MED_FORELDREPENGER", "Permisjon med foreldrepenger", "permisjonMedForeldrepenger"),
    PERMITTERING("PERMITTERING", "Permittering", "permittering"),
    PERMISJON_VED_MILITÆRTJENESTE("PERMISJON_VED_MILITÆRTJENESTE", "Permisjon ved militærtjeneste", "permisjonVedMilitaertjeneste"),
    ;

    private static final Set<PermisjonsbeskrivelseType> PERMISJON_IKKE_RELEVANT_FOR_AVKLAR_ARBEIDSFORHOLD = Set.of(
        PermisjonsbeskrivelseType.UTDANNINGSPERMISJON,
        PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_LOVFESTET,
        PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_IKKE_LOVFESTET,
        PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER
    );

    public static final Set<PermisjonsbeskrivelseType> PERMISJON_TILSVARENDE_VELFERDSPERIMISJON = Set.of(
        //VELFERDSPERMISJON utgår 31/12-2022 og er erstattes av de to andre her
        PermisjonsbeskrivelseType.VELFERDSPERMISJON,
        PermisjonsbeskrivelseType.ANNEN_PERMISJON_LOVFESTET,
        PermisjonsbeskrivelseType.ANNEN_PERMISJON_IKKE_LOVFESTET
    );

    private static final Map<String, PermisjonsbeskrivelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERMISJONSBESKRIVELSE_TYPE";

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
    @JsonIgnore
    private String offisiellKode;

    private PermisjonsbeskrivelseType(String kode) {
        this.kode = kode;
    }

    private PermisjonsbeskrivelseType(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static PermisjonsbeskrivelseType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(PermisjonsbeskrivelseType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PermisjonsbeskrivelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, PermisjonsbeskrivelseType> kodeMap() {
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
        return offisiellKode;
    }

    public boolean erRelevantForAvklarArbeidsforhold() {
        return !PERMISJON_IKKE_RELEVANT_FOR_AVKLAR_ARBEIDSFORHOLD.contains(this);
    }

}
