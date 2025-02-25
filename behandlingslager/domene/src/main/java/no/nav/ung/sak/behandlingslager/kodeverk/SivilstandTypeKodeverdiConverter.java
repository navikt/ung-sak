package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.person.SivilstandType;

@Converter(autoApply = true)
public class SivilstandTypeKodeverdiConverter implements AttributeConverter<SivilstandType, String> {
    @Override
    public String convertToDatabaseColumn(SivilstandType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public SivilstandType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SivilstandType.fraKode(dbData);
    }
}
