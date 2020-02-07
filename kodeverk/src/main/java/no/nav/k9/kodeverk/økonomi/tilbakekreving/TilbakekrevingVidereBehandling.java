package no.nav.k9.kodeverk.økonomi.tilbakekreving;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum TilbakekrevingVidereBehandling implements Kodeverdi {

    UDEFINIERT("-", "Udefinert."),
    TILBAKEKREV_I_INFOTRYGD("TILBAKEKR_INFOTRYGD", "Feilutbetaling med tilbakekreving"),
    IGNORER_TILBAKEKREVING("TILBAKEKR_IGNORER", "Feilutbetaling, avvent samordning"),
    INNTREKK("TILBAKEKR_INNTREKK", "Feilutbetaling hvor inntrekk dekker hele beløpet"),
    TILBAKEKR_OPPDATER("TILBAKEKR_OPPDATER", "Endringer vil oppdatere eksisterende feilutbetalte perioder og beløp."),
    ;

    private static final Map<String, TilbakekrevingVidereBehandling> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "TILBAKEKR_VIDERE_BEH";

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

    private TilbakekrevingVidereBehandling(String kode) {
        this.kode = kode;
    }

    private TilbakekrevingVidereBehandling(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static TilbakekrevingVidereBehandling fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent TilbakekrevingVidereBehandling: " + kode);
        }
        return ad;
    }

    public static Map<String, TilbakekrevingVidereBehandling> kodeMap() {
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
    
}