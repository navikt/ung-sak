package no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument;

import jakarta.persistence.AttributeConverter;
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentType;

public class OpplæringDokumentTypeConverter implements AttributeConverter<OpplæringDokumentType, String> {

    @Override
    public String convertToDatabaseColumn(OpplæringDokumentType type) {
        return type.getDatabasekode();
    }

    @Override
    public OpplæringDokumentType convertToEntityAttribute(String databasekode) {
        for (OpplæringDokumentType type : OpplæringDokumentType.values()) {
            if (type.getDatabasekode().equals(databasekode)) {
                return type;
            }
        }
        return null;
    }
}
