package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.person.SivilstandType;

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