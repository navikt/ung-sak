package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.aksjonspunkt.Ventekategori.*;

public enum Venteårsak implements Kodeverdi {

    UDEFINERT("-", "Ikke definert", false, null),

    AVV_DOK("AVV_DOK", "Annen dokumentasjon", true, AVVENTER_SØKER),
    FOR_TIDLIG_SOKNAD("FOR_TIDLIG_SOKNAD", "Venter pga for tidlig søknad", false, AVVENTER_ANNET_IKKE_SAKSBEHANDLINGSTID),
    VENT_INNTEKT_RAPPORTERINGSFRIST("VENT_INNTEKT_RAPPORTERINGSFRIST", "Inntekt rapporteringsfrist", false, AVVENTER_ARBEIDSGIVER),
    VENT_TIDLIGERE_BEHANDLING("VENT_TIDLIGERE_BEHANDLING", "Venter på iverksettelse av en tidligere behandling i denne saken", false, AVVENTER_ANNET),
    VENTER_SVAR_TEAMS("VENTER_SVAR_TEAMS", "Sak meldt i Teams, venter på svar", true, AVVENTER_TEKNISK_FEIL),
    ANNET("ANNET", "Annet", true, AVVENTER_ANNET),  //TODO?

    VENT_ØKONOMI("VENT_ØKONOMI", "Venter på økonomiløsningen", false, AVVENTER_ANNET),
    VENT_TILBAKEKREVING("VENT_TILBAKEKREVING", "Venter på tilbakekrevingsbehandling", true, AVVENTER_ANNET),

    VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE("VENTER_ETTERLYS_INNTEKT_UTTALELSE", "Venter på svar fra deltaker om avvik i registerinntekt", false, AVVENTER_SØKER),
    VENTER_BEKREFTELSE_ENDRET_UNGDOMSPROGRAMPERIODE("VENTER_BEKREFT_ENDRET_UNGDOMSPROGRAMPERIODE", "Venter på svar fra deltaker om endring i ungdomsprogramperiode", false, AVVENTER_SØKER),

    ;
    public static final String KODEVERK = "VENT_AARSAK";
    private static final Map<String, Venteårsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private boolean kanVelgesIGui;

    private String kode;

    private Ventekategori ventekategori;

    private Venteårsak(String kode) {
        this.kode = kode;
    }

    private Venteårsak(String kode, String navn, boolean kanVelgesIGui, Ventekategori ventekategori) {
        this.kode = kode;
        this.navn = navn;
        this.kanVelgesIGui = kanVelgesIGui;
        this.ventekategori = ventekategori;
    }

    public static Venteårsak fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Venteårsak: " + kode);
        }
        return ad;
    }

    public static Map<String, Venteårsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(a -> "\"" + a + "\"").collect(Collectors.toList()));
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public boolean getKanVelgesIGui() {
        return this.kanVelgesIGui;
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

    public Ventekategori getVentekategori() {
        return ventekategori;
    }
}
