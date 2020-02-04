package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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