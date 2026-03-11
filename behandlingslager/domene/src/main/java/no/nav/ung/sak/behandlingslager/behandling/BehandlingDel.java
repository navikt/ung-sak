package no.nav.ung.sak.behandlingslager.behandling;

enum BehandlingDel {

    HELE("HELE"),
    DEL_1("DEL_1"),
    DEL_2("DEL_2");

    private final String kode;

    BehandlingDel(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
