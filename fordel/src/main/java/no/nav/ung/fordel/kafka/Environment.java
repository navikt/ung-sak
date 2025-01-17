package no.nav.ung.fordel.kafka;

public enum Environment {
    PROD("p"), Q2("q2");

    private String kode;

    Environment(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
