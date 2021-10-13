package no.nav.k9.sak.kontrakt.krav;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum ÅrsakTilVurdering {

    MANUELT_REVURDERER_PERIODE("MANUELT_REVURDERER_PERIODE"),
    REVURDERER_BERØRT_PERIODE("REVURDERER_BERØRT_PERIODE"),
    ENDRING_FRA_BRUKER("ENDRING_FRA_BRUKER"),
    TRUKKET_KRAV("TRUKKET_KRAV"),
    REVURDERER_NY_INNTEKTSMELDING("REVURDERER_NY_INNTEKTSMELDING"),
    REVURDERER_ENDRING_FRA_ANNEN_PART("REVURDERER_ENDRING_FRA_ANNEN_PART"),
    G_REGULERING("G_REGULERING"),
    FØRSTEGANGSVURDERING("FØRSTEGANGSVURDERING");

    private static final Map<String, ÅrsakTilVurdering> KODER = new LinkedHashMap<>();
    private static final Map<BehandlingÅrsakType, ÅrsakTilVurdering> SAMMENHENG = Map.of(
        BehandlingÅrsakType.RE_SATS_REGULERING, G_REGULERING,
        BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON, REVURDERER_ENDRING_FRA_ANNEN_PART,
        BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING, REVURDERER_NY_INNTEKTSMELDING);

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonValue
    private final String kode;

    ÅrsakTilVurdering(String kode) {
        this.kode = kode;
    }

    @JsonCreator
    public static ÅrsakTilVurdering fraKode(String kode) {
        return KODER.get(kode);
    }

    public static ÅrsakTilVurdering mapFra(BehandlingÅrsakType type) {
        var årsakTilVurdering = SAMMENHENG.get(type);
        if (årsakTilVurdering != null) {
            return årsakTilVurdering;
        }
        throw new IllegalArgumentException("Ukjent type " + type);
    }
}
