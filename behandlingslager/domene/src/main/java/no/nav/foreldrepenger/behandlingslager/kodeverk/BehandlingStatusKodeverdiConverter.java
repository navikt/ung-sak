package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;

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