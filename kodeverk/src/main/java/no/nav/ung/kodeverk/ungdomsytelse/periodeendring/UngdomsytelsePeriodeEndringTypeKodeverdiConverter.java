package no.nav.ung.kodeverk.ungdomsytelse.periodeendring;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UngdomsytelsePeriodeEndringTypeKodeverdiConverter implements AttributeConverter<UngdomsytelsePeriodeEndringType, String> {

    @Override
    public String convertToDatabaseColumn(UngdomsytelsePeriodeEndringType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public UngdomsytelsePeriodeEndringType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UngdomsytelsePeriodeEndringType.fraKode(dbData);
    }
}
