package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;

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