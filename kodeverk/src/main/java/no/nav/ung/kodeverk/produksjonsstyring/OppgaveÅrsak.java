package no.nav.ung.kodeverk.produksjonsstyring;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum OppgaveÅrsak implements Kodeverdi {

    BEHANDLE_SAK_VL("BEH_SAK_VL", "Behandle sak i VL"),
    REVURDER_VL("RV_VL", "Revurdere i VL"),
    GODKJENN_VEDTAK_VL("GOD_VED_VL", "Godkjenne vedtak i VL"),
    REG_SOKNAD_VL("REG_SOK_VL", "Registrere søknad i VL"),
    VURDER_KONSEKVENS_YTELSE("VUR_KONS_YTE", "Vurder konsekvens for ytelse"),
    VURDER_DOKUMENT("VUR", "Vurder dokument"),
    FEILUTBETALING("FEILUTBET", "Feilutbetalingsvedtak"),
    INNHENT_DOK("INNH_DOK", "Innhent dokumentasjon"),
    SETTVENT("SETTVENT", "Sett utbetaling på vent"),
    BEHANDLE_SAK_IT("BEH_SAK", "Behandle sak"),
    UDEFINERT("-", "Ikke definert"),
    ;

    public static final String KODEVERK = "OPPGAVE_AARSAK";

    private static final Map<String, OppgaveÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;
    private String kode;

    private OppgaveÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static OppgaveÅrsak  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OppgaveÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, OppgaveÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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
