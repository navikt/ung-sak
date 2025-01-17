package no.nav.ung.fordel.kafka;

public enum Environment {
    PROD("p"), Q0("q0"), Q1("q1"), T4("t4");

    private String kode;

    Environment(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
