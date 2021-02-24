package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.AttributeConverter;

import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;

public class SykdomDokumentTypeConverter implements AttributeConverter<SykdomDokumentType, String> {
    @Override
    public String convertToDatabaseColumn(SykdomDokumentType type) {
        return type.getDatabasekode();
    }
    
    @Override
    public SykdomDokumentType convertToEntityAttribute(String databasekode) {
        for (SykdomDokumentType type : SykdomDokumentType.values()) {
            if (type.getDatabasekode().equals(databasekode)) {
                return type;
            }
        }
        return null;
    }
}