package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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