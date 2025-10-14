package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.varsel.EndringType;

@Converter(autoApply = true)
public class EndringTypeKodeverdiConverter implements AttributeConverter<EndringType, String> {
    @Override
    public String convertToDatabaseColumn(EndringType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public EndringType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EndringType.fraKode(dbData);
    }
}
