package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SykdomDokumentType {
    LEGEERKLÆRING_SYKEHUS("LEGEERKLÆRING_SYKEHUS", "L", true),
    MEDISINSKE_OPPLYSNINGER("MEDISINSKE_OPPLYSNINGER", "M", true),
    ANNET("ANNET", "A", false),
    UKLASSIFISERT("UKLASSIFISERT", "U", false);
    
    
    @JsonValue
    private final String apikode;
    
    @JsonIgnore
    private final String databasekode;
    
    @JsonIgnore
    private final boolean relevantForSykdom;
    
    
    SykdomDokumentType(String apikode, String databasekode, boolean relevantForSykdom) {
        this.apikode = apikode;
        this.databasekode = databasekode;
        this.relevantForSykdom = relevantForSykdom;
    }
    
    
    public boolean isRelevantForSykdom() {
        return relevantForSykdom;
    }
    
    @JsonCreator(mode = Mode.DELEGATING)
    public static SykdomDokumentType fraApikode(String s) {
        for (var type : values()) {
            if (type.apikode.equals(s)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Ukjent type: " + s);
    }
    
    
    public static class SykdomDokumentTypeConverter implements AttributeConverter<SykdomDokumentType, String> {
        @Override
        public String convertToDatabaseColumn(SykdomDokumentType type) {
            return type.databasekode;
        }
        
        @Override
        public SykdomDokumentType convertToEntityAttribute(String databasekode) {
            for (SykdomDokumentType type : values()) {
                if (type.databasekode.equals(databasekode)) {
                    return type;
                }
            }
            return null;
        }
    }
}
