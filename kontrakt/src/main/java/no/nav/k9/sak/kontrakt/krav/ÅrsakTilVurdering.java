package no.nav.k9.sak.kontrakt.krav;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum ÅrsakTilVurdering implements Kodeverdi {

    MANUELT_REVURDERER_PERIODE("MANUELT_REVURDERER_PERIODE", "Manuell revurdering"),
    // Ligger kant-i-kant med ny periode eller endring og blir dermed tatt med til vurdering
    REVURDERER_BERØRT_PERIODE("REVURDERER_BERØRT_PERIODE", "Tilstøtende periode"),
    // Tilkommet opplysninger på ny søknad fra bruker / punsj
    ENDRING_FRA_BRUKER("ENDRING_FRA_BRUKER", "Endring fra søknad/Punsj"),
    // Kravet for perioden har blitt trukket
    TRUKKET_KRAV("TRUKKET_KRAV", "Endret/fjernet søknadsperiode"),
    // Vurderes på nytt pga inntektsmelding som er sendt inn for perioden
    REVURDERER_NY_INNTEKTSMELDING("REVURDERER_NY_INNTEKTSMELDING", "Ny inntektsmelding"),
    // Perioden ses på pga endringer i felles opplysninger (Nattevåk/beredskap/etablert tilsyn/sykdom
    REVURDERER_ENDRING_FRA_ANNEN_PART("REVURDERER_ENDRING_FRA_ANNEN_PART", "Annen parts vedtak endrer uttak"),
    REVURDERER_ENDRING_FERIEPENGER("REVURDERER_ENDRING_FERIEPENGER_ANNEN_SAK", "Feriepenger reberegnes som følge av vedtak i annen sak"),
    UTSATT_BEHANDLING("UTSATT_BEHANDLING", "Utsatt behandling"),
    GJENOPPTAR_UTSATT_BEHANDLING("GJENOPPTAR_UTSATT_BEHANDLING", "Gjenopptar utsatt behandling"),
    // Endringer på felles opplysninger på sykdomsopplysningene
    REVURDERER_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON("REVURDERER_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON", "Endring i vurdering av sykdom"),
    // Endringer på felles opplysninger om etablert tilsyn
    REVURDERER_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON("REVURDERER_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON", "Endring i felles opplysninger om etablert tilsyn"),
    // Endringer på felles opplysninger om nattevåk/beredskap
    REVURDERER_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON("REVURDERER_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON", "Endring i felles opplysninger om nattevåk/beredskap"),
    HENDELSE_DØD_BRUKER("HENDELSE_DØD_BRUKER", "Dødsfall bruker"),
    HENDELSE_DØD_PLEIETRENGENDE("HENDELSE_DØD_PLEIETRENGENDE", "Dødsfall pleietrengende"),
    // Vurderes på nytt pga G_REGULERING
    G_REGULERING("G_REGULERING", "G-regulering"),
    REVURDERER_BEREGNING("REVURDERER_BEREGNING", "Endring opplysninger som påvirker beregningsgrunnlaget."),
    ENDRING_I_FORDELING("ENDRING_I_FORDELING", "Endring som påvirker fordeling og/eller gradering av ytelse."),

    // Vurderes for første gang
    FØRSTEGANGSVURDERING("FØRSTEGANGSVURDERING", "Ny periode"),
    // Vurdering etter klage på lignet inntekt
    REVURDERER_LIGNET_INNTEKT("REVURDERER_LIGNET_INNTEKT", "Manuell revurdering der lignet inntekt innhentes på nytt");

    private static final Map<String, ÅrsakTilVurdering> KODER = new LinkedHashMap<>();
    private static final Map<BehandlingÅrsakType, ÅrsakTilVurdering> SAMMENHENG;

    static {
        EnumMap<BehandlingÅrsakType, ÅrsakTilVurdering> sammenheng = new EnumMap<>(BehandlingÅrsakType.class);
        sammenheng.put(BehandlingÅrsakType.RE_SATS_REGULERING, G_REGULERING);
        sammenheng.put(BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON, REVURDERER_ENDRING_FRA_ANNEN_PART);
        sammenheng.put(BehandlingÅrsakType.RE_UTSATT_BEHANDLING, UTSATT_BEHANDLING);
        sammenheng.put(BehandlingÅrsakType.RE_GJENOPPTAR_UTSATT_BEHANDLING, GJENOPPTAR_UTSATT_BEHANDLING);
        sammenheng.put(BehandlingÅrsakType.RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON, REVURDERER_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        sammenheng.put(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON, REVURDERER_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        sammenheng.put(BehandlingÅrsakType.RE_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON, REVURDERER_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, HENDELSE_DØD_PLEIETRENGENDE);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, HENDELSE_DØD_BRUKER);
        sammenheng.put(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING, REVURDERER_NY_INNTEKTSMELDING);
        sammenheng.put(BehandlingÅrsakType.RE_FERIEPENGER_ENDRING_FRA_ANNEN_SAK, REVURDERER_ENDRING_FERIEPENGER);
        sammenheng.put(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_MEDLEMSKAP, REVURDERER_BERØRT_PERIODE);
        sammenheng.put(BehandlingÅrsakType.RE_KLAGE_NY_INNH_LIGNET_INNTEKT, REVURDERER_LIGNET_INNTEKT);
        sammenheng.put(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG, MANUELT_REVURDERER_PERIODE);
        sammenheng.put(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING, MANUELT_REVURDERER_PERIODE);
        sammenheng.put(BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG, REVURDERER_BEREGNING);
        sammenheng.put(BehandlingÅrsakType.RE_ENDRET_FORDELING, ENDRING_I_FORDELING);
        SAMMENHENG = Collections.unmodifiableMap(sammenheng);
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonValue
    private final String kode;
    private final String navn;

    ÅrsakTilVurdering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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

    public static Map<String, ÅrsakTilVurdering> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return "ÅRSAK_TIL_VURDERING";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
