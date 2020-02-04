package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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