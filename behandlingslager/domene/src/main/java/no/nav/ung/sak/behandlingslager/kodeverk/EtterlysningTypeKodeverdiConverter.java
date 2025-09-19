package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.varsel.EtterlysningType;

@Converter(autoApply = true)
public class EtterlysningTypeKodeverdiConverter implements AttributeConverter<EtterlysningType, String> {
    @Override
    public String convertToDatabaseColumn(EtterlysningType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public EtterlysningType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EtterlysningType.fraKode(dbData);
    }
}
