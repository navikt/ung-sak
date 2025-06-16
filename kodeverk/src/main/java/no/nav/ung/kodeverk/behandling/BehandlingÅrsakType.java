package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public enum BehandlingÅrsakType implements Kodeverdi {

    NY_SØKT_PROGRAM_PERIODE("RE-END-FRA-BRUKER", "Endring fra bruker"),
    RE_ANNET("RE-ANNET", "Annet"),
    RE_SATS_REGULERING("RE-SATS-REGULERING", "Regulering av grunnbeløp"),

    // Manuelt opprettet revurdering (obs: årsakene kan også bli satt på en automatisk opprettet revurdering)
    RE_KLAGE_UTEN_END_INNTEKT("RE-KLAG-U-INNTK", "Klage/ankebehandling uten endrede inntektsopplysninger"),
    RE_KLAGE_MED_END_INNTEKT("RE-KLAG-M-INNTK", "Klage/ankebehandling med endrede inntektsopplysninger"),
    RE_OPPLYSNINGER_OM_DØD("RE-DØD", "Nye opplysninger om brukers eller barns dødsfall"),
    ETTER_KLAGE("ETTER_KLAGE", "Ny behandling eller revurdering etter klage eller anke"),

    RE_HENDELSE_FØDSEL("RE-HENDELSE-FØDSEL", "Melding om registrert fødsel i folkeregisteret"),
    RE_HENDELSE_DØD_FORELDER("RE-HENDELSE-DØD-F", "Melding om registrert død på bruker i folkeregisteret"),
    RE_HENDELSE_DØD_BARN("RE-HENDELSE-DØD-B", "Melding om registrert død på pleietrengende i folkeregisteret"),
    RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM("RE-HENDELSE-OPPHØR-UNG", "Melding om registrert opphør av ungdomsprogram for bruker"),
    RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM("RE-HENDELSE-ENDRET-STARTDATO-UNG", "Melding om registrert endret startdato av ungdomsprogram for bruker"),

    RE_REGISTEROPPLYSNING("RE-REGISTEROPPL", "Nye registeropplysninger"),
    RE_INNTEKTSOPPLYSNING("RE-INNTEKTOPPL", "Nye opplysninger om inntekt"),

    //ungdomsytelsespesifikt
    RE_TRIGGER_BEREGNING_HØY_SATS("RE_TRIGGER_BEREGNING_HØY_SATS", "Beregn høy sats"),

    // Innrapportering av inntekt
    RE_RAPPORTERING_INNTEKT("RE-RAPPORTERING-INNTEKT", "Rapportering av inntekt"),
    RE_KONTROLL_REGISTER_INNTEKT("RE-KONTROLL-REGISTER-INNTEKT", "Kontroll av registerinntekt"),

    // Generell oppgavebekreftelse
    UTTALELSE_FRA_BRUKER("UTTALELSE-FRA-BRUKER", "Uttalelse fra bruker"),



    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "BEHANDLING_AARSAK"; //$NON-NLS-1$

    private static final Map<String, BehandlingÅrsakType> KODER = new LinkedHashMap<>();

    private String navn;

    private String kode;

    private BehandlingÅrsakType(String kode) {
        this.kode = kode;
    }

    private BehandlingÅrsakType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static BehandlingÅrsakType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingÅrsakType: for input " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingÅrsakType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    // Mulig relevant for klage
    public static Set<BehandlingÅrsakType> årsakerEtterKlageBehandling() {
        return Set.of(ETTER_KLAGE, RE_KLAGE_MED_END_INNTEKT, RE_KLAGE_UTEN_END_INNTEKT);
    }
}
