package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.geografisk.Region;

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
