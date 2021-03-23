package no.nav.k9.sak.kontrakt.sykdom;

public enum Resultat {
    OPPFYLT("OPPFYLT"),
    IKKE_OPPFYLT("IKKE_OPPFYLT");

    private final String kode;

    Resultat(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
