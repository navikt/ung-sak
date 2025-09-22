package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;

@Converter(autoApply = true)
public class EtterlysningStatusKodeverdiConverter implements AttributeConverter<EtterlysningStatus, String> {
    @Override
    public String convertToDatabaseColumn(EtterlysningStatus attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public EtterlysningStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EtterlysningStatus.fraKode(dbData);
    }
}
