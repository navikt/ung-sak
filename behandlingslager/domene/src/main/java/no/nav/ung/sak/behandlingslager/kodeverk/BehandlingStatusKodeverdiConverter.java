package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;

@Converter(autoApply = true)
public class BehandlingStatusKodeverdiConverter implements AttributeConverter<BehandlingStatus, String> {
    @Override
    public String convertToDatabaseColumn(BehandlingStatus attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BehandlingStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BehandlingStatus.fraKode(dbData);
    }
}
