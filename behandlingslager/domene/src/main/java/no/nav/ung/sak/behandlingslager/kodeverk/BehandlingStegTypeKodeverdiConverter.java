package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;

@Converter(autoApply = true)
public class BehandlingStegTypeKodeverdiConverter implements AttributeConverter<BehandlingStegType, String> {
    @Override
    public String convertToDatabaseColumn(BehandlingStegType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BehandlingStegType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BehandlingStegType.fraKode(dbData);
    }
}
