package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

@Converter(autoApply = true)
public class UngdomsytelseSatsTypeKodeverdiConverter implements AttributeConverter<UngdomsytelseSatsType, String> {

    @Override
    public String convertToDatabaseColumn(UngdomsytelseSatsType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public UngdomsytelseSatsType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UngdomsytelseSatsType.fraKode(dbData);
    }
}
