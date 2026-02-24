package no.nav.ung.sak.kontrakt.oppgaver;

import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public enum OppgaveType implements Kodeverdi {

    BEKREFT_ENDRET_STARTDATO(OppgaveTypeKoder.BEKREFT_ENDRET_STARTDATO_KODE, "Bekreft endret startdato"),
    BEKREFT_ENDRET_SLUTTDATO(OppgaveTypeKoder.BEKREFT_ENDRET_SLUTTDATO_KODE, "Bekreft endret sluttdato"),
    BEKREFT_ENDRET_PERIODE(OppgaveTypeKoder.BEKREFT_ENDRET_PERIODE_KODE, "Bekreft endret periode"),
    BEKREFT_FJERNET_PERIODE(OppgaveTypeKoder.BEKREFT_FJERNET_PERIODE_KODE, "Bekreft fjernet periode"),
    BEKREFT_AVVIK_REGISTERINNTEKT(OppgaveTypeKoder.BEKREFT_AVVIK_REGISTERINNTEKT_KODE, "Bekreft avvik registerinntekt"),
    RAPPORTER_INNTEKT(OppgaveTypeKoder.RAPPORTER_INNTEKT_KODE, "Rapporter inntekt"),
    SØK_YTELSE(OppgaveTypeKoder.SØK_YTELSE_KODE, "Søk ytelse")
    ;

    private static final Map<String, OppgaveType> KODER = new LinkedHashMap<>();


    static {
        // valider ingen unmapped koder
        var sjekkKodeBrukMap = new TreeMap<>(OppgaveTypeKoder.KODER);

        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode + ", mulig utgått?");
            }
            if (v.kode != null) {
                sjekkKodeBrukMap.remove(v.kode);
            }
        }

        if (!sjekkKodeBrukMap.isEmpty()) {
            System.out.printf("Ubrukt sjekk: Har koder definert i %s som ikke er i bruk i %s: %s\n", OppgaveTypeKoder.class, OppgaveType.class, sjekkKodeBrukMap);
        }
    }

    private String kode;
    private String navn;

    OppgaveType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static OppgaveType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OppgaveType: " + kode);
        }
        return ad;
    }


    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return "OPPGAVE_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
