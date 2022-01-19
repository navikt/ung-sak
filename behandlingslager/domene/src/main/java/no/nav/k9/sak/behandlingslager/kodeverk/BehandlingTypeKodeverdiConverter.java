package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.behandling.BehandlingType;

@Converter(autoApply = true)
public class BehandlingTypeKodeverdiConverter implements AttributeConverter<BehandlingType, String> {
    @Override
    public String convertToDatabaseColumn(BehandlingType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BehandlingType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BehandlingType.fraKode(dbData);
    }
}
