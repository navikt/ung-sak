package no.nav.k9.sak.kontrakt.sykdom;

public enum SykdomVurderingType {
    KONTINUERLIG_TILSYN_OG_PLEIE("KTP"),
    TO_OMSORGSPERSONER("TOO");
    
    private final String kode;
    
    SykdomVurderingType(String kode) {
        this.kode = kode;
    }
    
    public String getKode() {
        return kode;
    }
}
