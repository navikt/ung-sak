package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.person.PersonstatusType;

@Converter(autoApply = true)
public class PersonstatusKodeverdiConverter implements AttributeConverter<PersonstatusType, String> {
    @Override
    public String convertToDatabaseColumn(PersonstatusType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public PersonstatusType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PersonstatusType.fraKode(dbData);
    }
}
