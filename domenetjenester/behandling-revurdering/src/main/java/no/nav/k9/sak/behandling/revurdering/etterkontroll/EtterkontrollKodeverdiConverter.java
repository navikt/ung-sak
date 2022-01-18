package no.nav.k9.sak.behandling.revurdering.etterkontroll;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class EtterkontrollKodeverdiConverter implements AttributeConverter<KontrollType, String> {
    @Override
    public String convertToDatabaseColumn(KontrollType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public KontrollType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KontrollType.fraKode(dbData);
    }
}