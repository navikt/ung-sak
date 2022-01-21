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
    // Ligger kant-i-kant med ny periode eller endring og blir dermed tatt med til vurdering
    REVURDERER_BERØRT_PERIODE("REVURDERER_BERØRT_PERIODE"),
    // Tilkommet opplysninger på ny søknad fra bruker / punsj
    ENDRING_FRA_BRUKER("ENDRING_FRA_BRUKER"),
    // Kravet for perioden har blitt trukket
    TRUKKET_KRAV("TRUKKET_KRAV"),
    // Vurderes på nytt pga inntektsmelding som er sendt inn for perioden
    REVURDERER_NY_INNTEKTSMELDING("REVURDERER_NY_INNTEKTSMELDING"),
    // Perioden ses på pga endringer i felles opplysninger (Nattevåk/beredskap/etablert tilsyn/sykdom
    REVURDERER_ENDRING_FRA_ANNEN_PART("REVURDERER_ENDRING_FRA_ANNEN_PART"),
    // Endringer på felles opplysninger på sykdomsopplysningene
    REVURDERER_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON("REVURDERER_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON"),
    // Endringer på felles opplysninger om etablert tilsyn
    REVURDERER_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON("REVURDERER_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON"),
    // Endringer på felles opplysninger om nattevåk/beredskap
    REVURDERER_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON("REVURDERER_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON"),
    // Vurderes på nytt pga G_REGULERING
    G_REGULERING("G_REGULERING"),
    // Vurderes for første gang
    FØRSTEGANGSVURDERING("FØRSTEGANGSVURDERING");

    private static final Map<String, ÅrsakTilVurdering> KODER = new LinkedHashMap<>();
    private static final Map<BehandlingÅrsakType, ÅrsakTilVurdering> SAMMENHENG = Map.of(
        BehandlingÅrsakType.RE_SATS_REGULERING, G_REGULERING,
        BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON, REVURDERER_ENDRING_FRA_ANNEN_PART,
        BehandlingÅrsakType.RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON, REVURDERER_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON,
        BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON, REVURDERER_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON,
        BehandlingÅrsakType.RE_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON, REVURDERER_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON,
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
