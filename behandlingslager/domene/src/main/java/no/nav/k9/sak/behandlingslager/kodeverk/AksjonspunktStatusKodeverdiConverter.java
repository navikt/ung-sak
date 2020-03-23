package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;

@Converter(autoApply = true)
public class AksjonspunktStatusKodeverdiConverter implements AttributeConverter<AksjonspunktStatus, String> {
    @Override
    public String convertToDatabaseColumn(AksjonspunktStatus attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public AksjonspunktStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AksjonspunktStatus.fraKode(dbData);
    }
}