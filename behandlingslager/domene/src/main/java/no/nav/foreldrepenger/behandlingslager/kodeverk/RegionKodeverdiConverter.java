package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.geografisk.Region;

@Converter(autoApply = true)
public class RegionKodeverdiConverter implements AttributeConverter<Region, String> {
    @Override
    public String convertToDatabaseColumn(Region attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Region convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Region.fraKode(dbData);
    }
}