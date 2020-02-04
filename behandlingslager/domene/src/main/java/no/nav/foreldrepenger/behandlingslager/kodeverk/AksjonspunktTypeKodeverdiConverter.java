package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktType;

@Converter(autoApply = true)
public class AksjonspunktTypeKodeverdiConverter implements AttributeConverter<AksjonspunktType, String> {
    @Override
    public String convertToDatabaseColumn(AksjonspunktType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public AksjonspunktType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AksjonspunktType.fraKode(dbData);
    }
}