package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;

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
