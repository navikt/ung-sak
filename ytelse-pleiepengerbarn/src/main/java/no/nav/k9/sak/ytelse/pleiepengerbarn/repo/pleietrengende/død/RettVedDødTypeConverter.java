package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død;

import no.nav.k9.kodeverk.uttak.RettVedDødType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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
