package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død;

import no.nav.k9.kodeverk.uttak.RettVedDødType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RettVedDødTypeConverter implements AttributeConverter<RettVedDødType, String> {
    @Override
    public String convertToDatabaseColumn(RettVedDødType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public RettVedDødType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RettVedDødType.fraKode(dbData);
    }
}
