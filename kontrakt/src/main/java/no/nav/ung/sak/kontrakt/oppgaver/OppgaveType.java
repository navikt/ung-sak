package no.nav.ung.sak.kontrakt.oppgaver;

import no.nav.ung.kodeverk.api.Kodeverdi;

public enum OppgaveType implements Kodeverdi {

    BEKREFT_ENDRET_STARTDATO(OppgaveTypeKoder.BEKREFT_ENDRET_STARTDATO_KODE, "Bekreft endret startdato"),
    BEKREFT_ENDRET_SLUTTDATO(OppgaveTypeKoder.BEKREFT_ENDRET_SLUTTDATO_KODE, "Bekreft endret sluttdato"),
    BEKREFT_ENDRET_PERIODE(OppgaveTypeKoder.BEKREFT_ENDRET_PERIODE_KODE, "Bekreft endret periode"),
    BEKREFT_FJERNET_PERIODE(OppgaveTypeKoder.BEKREFT_FJERNET_PERIODE_KODE, "Bekreft fjernet periode"),
    BEKREFT_AVVIK_REGISTERINNTEKT(OppgaveTypeKoder.BEKREFT_AVVIK_REGISTERINNTEKT_KODE, "Bekreft avvik registerinntekt"),
    RAPPORTER_INNTEKT(OppgaveTypeKoder.RAPPORTER_INNTEKT_KODE, "Rapporter inntekt"),
    SØK_YTELSE(OppgaveTypeKoder.SØK_YTELSE_KODE, "Søk ytelse")
    ;

    private String kode;
    private String navn;

    OppgaveType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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
