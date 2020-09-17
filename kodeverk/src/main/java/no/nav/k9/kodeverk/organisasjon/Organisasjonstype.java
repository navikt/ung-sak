package no.nav.k9.kodeverk.organisasjon;

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
import no.nav.k9.sak.typer.OrgNummer;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum Organisasjonstype implements Kodeverdi {

    JURIDISK_ENHET("JURIDISK_ENHET", "Juridisk enhet"),
    ORGLEDD("ORGANISASJONSLEDD", "Organisasjonsledd"),
    VIRKSOMHET("VIRKSOMHET", "Virksomhet"),
    KUNSTIG("KUNSTIG", "Kunstig arbeidsforhold lagt til av saksbehandler"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, Organisasjonstype> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ORGANISASJONSTYPE";

    @Deprecated
    public static final String DISCRIMINATOR = "ORGANISASJONSTYPE";

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

    private Organisasjonstype(String kode) {
        this.kode = kode;
    }

    private Organisasjonstype(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static Organisasjonstype  fraKode(@JsonProperty("kode") Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Organisasjonstype.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Organisasjonstype: " + kode);
        }
        return ad;
    }

    public static Map<String, Organisasjonstype> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static boolean erKunstig(String orgNr) {
        return OrgNummer.KUNSTIG_ORG.equals(orgNr);
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
}
