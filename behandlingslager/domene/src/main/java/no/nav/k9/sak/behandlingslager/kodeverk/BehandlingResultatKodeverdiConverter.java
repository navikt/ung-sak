package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;

@Converter(autoApply = true)
public class BehandlingResultatKodeverdiConverter implements AttributeConverter<BehandlingResultatType, String> {
    @Override
    public String convertToDatabaseColumn(BehandlingResultatType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BehandlingResultatType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BehandlingResultatType.fraKode(dbData);
    }
}
