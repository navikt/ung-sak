package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum SkjermlenkeType implements Kodeverdi {
    BEREGNING("BEREGNING", "Beregning"),
    FAKTA_OM_SIMULERING("FAKTA_OM_SIMULERING", "Simulering"),
    SOEKNADSFRIST("SOEKNADSFRIST", "Søknadsfrist"),
    UDEFINERT("-", "Ikke definert"),
    OPPLYSNINGSPLIKT("OPPLYSNINGSPLIKT", "Opplysningsplikt"),
    TILKJENT_YTELSE("TILKJENT_YTELSE", "Tilkjent ytelse"),
    VEDTAK("VEDTAK", "Vedtak"),
    ;

    private static final Map<String, SkjermlenkeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "SKJERMLENKE_TYPE"; //$NON-NLS-1$


    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private SkjermlenkeType(String kode) {
        this.kode = kode;
    }

    private SkjermlenkeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static SkjermlenkeType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent SkjermlenkeType: " + kode);
        }
        return ad;
    }

    public static Map<String, SkjermlenkeType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonValue
    @Override
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

    /**
     * Returnerer skjermlenketype for eit aksjonspunkt.
     *
     * @deprecated Brukes kun i totrinnskontroll og foreslå vedtak, bør også fjernes derfra og heller lagres på Aksjonspunktet (ikke definisjonen)
     */
    @Deprecated
    public static SkjermlenkeType finnSkjermlenkeType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return aksjonspunktDefinisjon.getSkjermlenkeType();
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<SkjermlenkeType, String> {
        @Override
        public String convertToDatabaseColumn(SkjermlenkeType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public SkjermlenkeType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
