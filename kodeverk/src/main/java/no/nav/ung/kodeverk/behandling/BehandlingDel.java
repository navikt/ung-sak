package no.nav.ung.kodeverk.behandling;

import no.nav.ung.kodeverk.api.Kodeverdi;

public enum BehandlingDel implements Kodeverdi {

    SENTRAL("SENTRAL", "Del av behandling som utføres i en sentral instans"),
    LOKAL("LOKAL", "Del av behandling som utføres ved et lokalkontor"),
    ;

    private final String kode;
    private final String navn;

    BehandlingDel(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return "BEHANDLING_DEL";
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
