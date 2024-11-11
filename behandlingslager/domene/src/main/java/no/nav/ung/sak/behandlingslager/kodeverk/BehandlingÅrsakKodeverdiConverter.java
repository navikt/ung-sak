package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@Converter(autoApply = true)
public class BehandlingÅrsakKodeverdiConverter implements AttributeConverter<BehandlingÅrsakType, String> {
    @Override
    public String convertToDatabaseColumn(BehandlingÅrsakType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BehandlingÅrsakType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BehandlingÅrsakType.fraKode(dbData);
    }
}
