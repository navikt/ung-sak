package no.nav.ung.kodeverk.ungdomsytelse.periodeendring;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UngdomsprogramPeriodeEndringTypeKodeverdiConverter implements AttributeConverter<UngdomsprogramPeriodeEndringType, String> {

    @Override
    public String convertToDatabaseColumn(UngdomsprogramPeriodeEndringType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public UngdomsprogramPeriodeEndringType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UngdomsprogramPeriodeEndringType.fraKode(dbData);
    }
}
