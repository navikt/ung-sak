package no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum Vurdering implements Kodeverdi {

    UDEFINERT("-", "Udefinert"),
    KAN_FORTSETTE("FORTSETT", "Kan beregnes på bakgrunn av opplysninger fra a-ordningen"),
    MANGLENDE_GRUNNLAG("MANGLENDE_GRUNNLAG", "Kan ikke forsett pga manglende grunnlag for å kunne beregne."),
    UAVKLART("UAVKLART", "Uavklart");

    private static final Map<String, Vurdering> KODER = new LinkedHashMap<>();
    public static final String KODEVERK = "KOMPLETTHET_VURDERING";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;
    private String beskrivelse;

    Vurdering(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Vurdering fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Vurdering.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Venteårsak: " + kode);
        }
        return ad;
    }

    public static Map<String, Vurdering> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return getKode();
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
