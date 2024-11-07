package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;

@Converter(autoApply = true)
public class VurderÅrsakKodeverdiConverter implements AttributeConverter<VurderÅrsak, String> {
    @Override
    public String convertToDatabaseColumn(VurderÅrsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public VurderÅrsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VurderÅrsak.fraKode(dbData);
    }
}
