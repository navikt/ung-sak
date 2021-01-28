package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.AttributeConverter;

public enum SykdomDokumentType {
    LEGEERKLÆRING_SYKEHUS("L"),
    MEDISINSKE_OPPLYSNINGER("M"),
    ANNET("A"),
    UKLASSIFISERT("U");
    
    private final String kode;
    
    SykdomDokumentType(String kode) {
        this.kode = kode;
    }
    
    public String getKode() {
        return kode;
    }
    
    
    public static class SykdomDokumentTypeConverter implements AttributeConverter<SykdomDokumentType, String> {
        @Override
        public String convertToDatabaseColumn(SykdomDokumentType type) {
            return type.kode;
        }
        
        @Override
        public SykdomDokumentType convertToEntityAttribute(String kode) {
            for (SykdomDokumentType type : values()) {
                if (type.kode.equals(kode)) {
                    return type;
                }
            }
            return null;
        }
    }
}
