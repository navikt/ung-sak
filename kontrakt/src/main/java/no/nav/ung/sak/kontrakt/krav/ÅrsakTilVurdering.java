package no.nav.ung.sak.kontrakt.krav;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.LegacyKodeverdiJsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@LegacyKodeverdiJsonValue() // <- Denne Kodeverdi har alltid blitt serialisert til kode string
public enum ÅrsakTilVurdering implements Kodeverdi {

    HENDELSE_DØD_BRUKER("HENDELSE_DØD_BRUKER", "Dødsfall deltaker"),
    HENDELSE_DØD_BARN("HENDELSE_DØD_BARN", "Dødsfall barn"),
    HENDELSE_FØDSEL_BARN("HENDELSE_FØDSEL_BARN", "Fødsel barn"),
    OPPHØR_UNGDOMSPROGRAM("OPPHØR_UNGDOMSPROGRAM", "Opphør av ungdomsprogram"),
    OPPHØR_VED_MAKSDATO("OPPHØR_VED_MAKSDATO", "Opphør ved maksdato"),
    /**
     * Et tidligere opphør ble faktisk vedtatt/iverksatt (bruker mottok opphørsbrev) og er nå reversert
     * ved en ny hendelse. Se {@link no.nav.ung.sak.kontrakt.behandling.BehandlingVisningsnavn#OPPHØR_OPPHEVET}.
     */
    OPPHØR_OPPHEVET_UNGDOMSPROGRAM("OPPHØR_OPPHEVET_UNGDOMSPROGRAM", "Opphevelse av opphør av ungdomsprogram"),
    /**
     * Et opphør ble avbrutt før det noen gang ble vedtatt/iverksatt (ingen opphørsbrev ble sendt).
     * Brukes kun som sporingsårsak i periodevisning/behandlingsoversikt, ikke koblet til noe vedtaksbrev.
     * Se {@link no.nav.ung.sak.kontrakt.behandling.BehandlingVisningsnavn#OPPHØR_ANNULERT}.
     */
    OPPHØR_ANNULERT_UNGDOMSPROGRAM("OPPHØR_ANNULERT_UNGDOMSPROGRAM", "Opphør annullert"),
    ENDRET_STARTDATO_UNGDOMSPROGRAM("ENDRET_STARTDATO_UNGDOMSPROGRAM", "Endret startdato for ungdomsprogram"),
    FORLENGET_PERIODE_UNGDOMSPROGRAM("FORLENGET_PERIODE_UNGDOMSPROGRAM", "Forlenget periode"),
    KONTROLL_AV_INNTEKT("KONTROLL_AV_INNTEKT", "Kontroll og rapportering av inntekt"),
    OVERGANG_HØY_SATS("OVERGANG_HØY_SATS", "Overgang til høy sats"),

    // Vurderes på nytt pga G_REGULERING
    G_REGULERING("G_REGULERING", "G-regulering"),
    // Vurderes for første gang
    FØRSTEGANGSVURDERING("FØRSTEGANGSVURDERING", "Ny periode");
    private static final Map<String, ÅrsakTilVurdering> KODER = new LinkedHashMap<>();
    private static final Map<BehandlingÅrsakType, ÅrsakTilVurdering> SAMMENHENG;

    static {
        EnumMap<BehandlingÅrsakType, ÅrsakTilVurdering> sammenheng = new EnumMap<>(BehandlingÅrsakType.class);
        sammenheng.put(BehandlingÅrsakType.RE_SATS_REGULERING, G_REGULERING);;
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, HENDELSE_DØD_BRUKER);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, HENDELSE_DØD_BARN);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, HENDELSE_FØDSEL_BARN);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, OPPHØR_UNGDOMSPROGRAM);
        sammenheng.put(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, OPPHØR_VED_MAKSDATO);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM, OPPHØR_OPPHEVET_UNGDOMSPROGRAM);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, ENDRET_STARTDATO_UNGDOMSPROGRAM);
        sammenheng.put(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM, FORLENGET_PERIODE_UNGDOMSPROGRAM);
        sammenheng.put(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, KONTROLL_AV_INNTEKT);
        sammenheng.put(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, KONTROLL_AV_INNTEKT);
        sammenheng.put(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, OVERGANG_HØY_SATS);
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
