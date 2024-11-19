package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;

@Converter(autoApply = true)
public class BehandlingStegStatusKodeverdiConverter implements AttributeConverter<BehandlingStegStatus, String> {
    @Override
    public String convertToDatabaseColumn(BehandlingStegStatus attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BehandlingStegStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BehandlingStegStatus.fraKode(dbData);
    }
}
