package no.nav.ung.kodeverk.organisasjon;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.sak.typer.OrgNummer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum Organisasjonstype implements Kodeverdi {

    JURIDISK_ENHET("JURIDISK_ENHET", "Juridisk enhet"),
    ORGLEDD("ORGANISASJONSLEDD", "Organisasjonsledd"),
    VIRKSOMHET("VIRKSOMHET", "Virksomhet"),
    KUNSTIG("KUNSTIG", "Kunstig arbeidsforhold lagt til av saksbehandler"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, Organisasjonstype> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ORGANISASJONSTYPE";

    @Deprecated
    public static final String DISCRIMINATOR = "ORGANISASJONSTYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private Organisasjonstype(String kode) {
        this.kode = kode;
    }

    private Organisasjonstype(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Organisasjonstype  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Organisasjonstype: " + kode);
        }
        return ad;
    }

    public static Map<String, Organisasjonstype> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static boolean erKunstig(String orgNr) {
        return OrgNummer.KUNSTIG_ORG.equals(orgNr);
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
