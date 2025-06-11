package no.nav.ung.kodeverk.historikk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum HistorikkEndretFeltVerdiType implements Kodeverdi {

    /** Medlemskap og Utenlandstilsnitt. */
    BOSATT_I_NORGE("BOSATT_I_NORGE", "Søker er bosatt i Norge"),
    BOSATT_UTLAND("BOSATT_UTLAND", "Bosatt utland"),
    EØS_BOSATT_NORGE("EØS_BOSATT_NORGE", "EØS bosatt Norge"),
    IKKE_BOSATT_I_NORGE("IKKE_BOSATT_I_NORGE", "Søker er ikke bosatt i Norge"),
    IKKE_LOVLIG_OPPHOLD("IKKE_LOVLIG_OPPHOLD", "Søker har ikke lovlig opphold"),
    IKKE_OPPHOLDSRETT("IKKE_OPPHOLDSRETT", "Søker har ikke oppholdsrett"),
    LOVLIG_OPPHOLD("LOVLIG_OPPHOLD", "Søker har lovlig opphold"),
    OPPHOLDSRETT("OPPHOLDSRETT", "Søker har oppholdsrett"),
    NASJONAL("NASJONAL", "Nasjonal"),

    /** Behandling */
    FORTSETT_BEHANDLING("FORTSETT_BEHANDLING", "Fortsett behandling"),
    HENLEGG_BEHANDLING("HENLEGG_BEHANDLING", "Henlegg behandling"),

    /** Tilbaketrekk */
    HINDRE_TILBAKETREKK("HINDRE_TILBAKETREKK", "Ikke tilbakekrev fra søker"),
    UTFØR_TILBAKETREKK("UTFØR_TILBAKETREKK", "Tilbakekrev fra søker"),

    /** Arbeid. */
    IKKE_NY_I_ARBEIDSLIVET("IKKE_NY_I_ARBEIDSLIVET", "til ikke ny i arbeidslivet"),
    IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD("IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD", "ikke tidsbegrenset"),
    NY_I_ARBEIDSLIVET("NY_I_ARBEIDSLIVET", "ny i arbeidslivet"),
    TIDSBEGRENSET_ARBEIDSFORHOLD("TIDSBEGRENSET_ARBEIDSFORHOLD", "tidsbegrenset arbeidsforhold"),

    /** Beregning. */
    VARIG_ENDRET_NAERING("VARIG_ENDRET_NAERING", "Varig endret næring"),
    INGEN_VARIG_ENDRING_NAERING("INGEN_VARIG_ENDRING_NAERING", "Ingen varig endring i næring"),
    NYOPPSTARTET("NYOPPSTARTET", "nyoppstartet"),
    IKKE_NYOPPSTARTET("IKKE_NYOPPSTARTET", "ikke nyoppstartet"),
    BENYTT("BENYTT", "Benytt"),
    IKKE_BENYTT("IKKE_BENYTT", "Ikke benytt"),

    /** Vilkår. */
    VILKAR_IKKE_OPPFYLT("VILKAR_IKKE_OPPFYLT", "Vilkåret er ikke oppfylt"),
    VILKAR_OPPFYLT("VILKAR_OPPFYLT", "Vilkåret er oppfylt"),

    /** Vilkår - Søkers opplysningsplikt. */
    IKKE_OPPFYLT("IKKE_OPPFYLT", "ikke oppfylt"),
    OPPFYLT("OPPFYLT", "oppfylt"),

    /** Vilkår - Opptjening. */
    OPPFYLT_8_47_A("OPPFYLT_8_47_A", "oppfylt jf § 8-47 bokstav A"),
    OPPFYLT_8_47_B("OPPFYLT_8_47_B", "oppfylt jf § 8-47 bokstav B"),

    /** Risk. */
    INGEN_INNVIRKNING("INGEN_INNVIRKNING", "Faresignalene hadde ingen innvirkning på behandlingen"),
    INNVIRKNING("INNVIRKNING", "Faresignalene hadde innvirkning på behandlingen"),

    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, HistorikkEndretFeltVerdiType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_ENDRET_FELT_VERDI_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private HistorikkEndretFeltVerdiType(String kode) {
        this.kode = kode;
    }

    private HistorikkEndretFeltVerdiType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static HistorikkEndretFeltVerdiType  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkEndretFeltVerdiType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkEndretFeltVerdiType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
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

}
