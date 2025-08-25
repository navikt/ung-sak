package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;

@Converter(autoApply = true)
public class SkjermlenkeTypeKodeverdiConverter implements AttributeConverter<SkjermlenkeType, String> {
    @Override
    public String convertToDatabaseColumn(SkjermlenkeType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public SkjermlenkeType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SkjermlenkeType.fraKode(dbData);
    }
}
