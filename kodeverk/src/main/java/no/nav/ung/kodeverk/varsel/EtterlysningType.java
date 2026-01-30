package no.nav.ung.kodeverk.varsel;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.LegacyKodeverdiJsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;

import java.util.LinkedHashMap;
import java.util.Map;

import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

@LegacyKodeverdiJsonValue // Serialiserast som kode string i default object mapper
public enum EtterlysningType implements Kodeverdi {

    UTTALELSE_KONTROLL_INNTEKT("UTTALELSE_KONTROLL_INNTEKT", "Svar på varsel: Avvik i registerinntekt"),
    UTTALELSE_ENDRET_STARTDATO("UTTALELSE_ENDRET_STARTDATO", "Svar på varsel: Endret startdato"),
    UTTALELSE_ENDRET_SLUTTDATO("UTTALELSE_ENDRET_SLUTTDATO", "Svar på varsel: Endret sluttdato"),
    UTTALELSE_ENDRET_PERIODE("UTTALELSE_ENDRET_PERIODE", "Svar på varsel: Endret programperiode"),

    ;

    @JsonValue
    private final String kode;
    private final String navn;


    private static final Map<String, EtterlysningType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


    EtterlysningType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static EtterlysningType fraKode(String kode) {
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent EtterlysningType: " + kode);
        }
        return ad;
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
        return "ETTERLYSNING_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public AksjonspunktDefinisjon tilAutopunktDefinisjon() {
        switch (this) {
            case UTTALELSE_KONTROLL_INNTEKT -> {
                return AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE;
            }
            case UTTALELSE_ENDRET_STARTDATO, UTTALELSE_ENDRET_SLUTTDATO, UTTALELSE_ENDRET_PERIODE -> {
                return AUTO_SATT_PÅ_VENT_REVURDERING;
            }
            default -> throw new IllegalArgumentException("Ukjent etterlysningstype: " + this);
        }
    }

    public Venteårsak mapTilVenteårsak() {
        switch (this) {
            case UTTALELSE_KONTROLL_INNTEKT -> {
                return Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE;
            }
            case UTTALELSE_ENDRET_STARTDATO, UTTALELSE_ENDRET_SLUTTDATO, UTTALELSE_ENDRET_PERIODE -> {
                return Venteårsak.VENTER_BEKREFTELSE_ENDRET_UNGDOMSPROGRAMPERIODE;
            }
            default -> throw new IllegalArgumentException("Ukjent etterlysningstype: " + this);
        }
    }

}
