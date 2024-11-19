package no.nav.ung.kodeverk.sykdom;

public enum Resultat {
    OPPFYLT("OPPFYLT"),
    IKKE_OPPFYLT("IKKE_OPPFYLT"),
    IKKE_VURDERT("IKKE_VURDERT");

    private final String kode;

    Resultat(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
