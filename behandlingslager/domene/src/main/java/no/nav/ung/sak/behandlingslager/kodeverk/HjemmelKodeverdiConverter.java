package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;

@Converter(autoApply = true)
public class HjemmelKodeverdiConverter implements AttributeConverter<Hjemmel, String> {

    @Override
    public String convertToDatabaseColumn(Hjemmel attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Hjemmel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Hjemmel.fraKode(dbData);
    }
}
