package no.nav.ung.sak.behandlingslager.aktør;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
//kjedelig at denne ikke kan være typet med AktørId, men den virker String-aktørId-feltet inne i AktørId
public class AktørIdConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData;
    }
}
