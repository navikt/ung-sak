package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.AttributeConverter;

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
    
    
    public static class SykdomVurderingTypeConverter implements AttributeConverter<SykdomVurderingType, String> {
        @Override
        public String convertToDatabaseColumn(SykdomVurderingType type) {
            return type.kode;
        }
        
        @Override
        public SykdomVurderingType convertToEntityAttribute(String kode) {
            for (SykdomVurderingType type : values()) {
                if (type.kode.equals(kode)) {
                    return type;
                }
            }
            return null;
        }
    }
}
