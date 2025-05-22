package no.nav.ung.sak.kontrakt.krav;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum ÅrsakTilVurdering implements Kodeverdi {

    ENDRING_FRA_BRUKER("ENDRING_FRA_BRUKER", "Endring fra søknad/Punsj"),
    HENDELSE_DØD_BRUKER("HENDELSE_DØD_BRUKER", "Dødsfall bruker"),
    HENDELSE_DØD_BARN("HENDELSE_DØD_BARN", "Dødsfall barn"),
    OPPHØR_UNGDOMSPROGRAM("OPPHØR_UNGDOMSPROGRAM", "Opphør av ungdomsprogram"),
    // Vurderes på nytt pga G_REGULERING
    G_REGULERING("G_REGULERING", "G-regulering"),
    REVURDERER_BEREGNING("REVURDERER_BEREGNING", "Endring opplysninger som påvirker beregningsgrunnlaget."),
    // Vurderes for første gang
    FØRSTEGANGSVURDERING("FØRSTEGANGSVURDERING", "Ny periode");
    private static final Map<String, ÅrsakTilVurdering> KODER = new LinkedHashMap<>();
    private static final Map<BehandlingÅrsakType, ÅrsakTilVurdering> SAMMENHENG;

    static {
        EnumMap<BehandlingÅrsakType, ÅrsakTilVurdering> sammenheng = new EnumMap<>(BehandlingÅrsakType.class);
        sammenheng.put(BehandlingÅrsakType.RE_SATS_REGULERING, G_REGULERING);;
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, HENDELSE_DØD_BRUKER);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, HENDELSE_DØD_BARN);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, OPPHØR_UNGDOMSPROGRAM);
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
