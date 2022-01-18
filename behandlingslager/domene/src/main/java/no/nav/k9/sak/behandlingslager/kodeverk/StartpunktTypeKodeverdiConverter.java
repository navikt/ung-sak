package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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