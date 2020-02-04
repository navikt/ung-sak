package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;

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