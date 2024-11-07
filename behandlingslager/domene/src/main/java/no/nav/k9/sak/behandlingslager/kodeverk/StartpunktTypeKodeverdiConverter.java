package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

@Converter(autoApply = true)
public class StartpunktTypeKodeverdiConverter implements AttributeConverter<StartpunktType, String> {
    @Override
    public String convertToDatabaseColumn(StartpunktType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public StartpunktType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : StartpunktType.fraKode(dbData);
    }
}
