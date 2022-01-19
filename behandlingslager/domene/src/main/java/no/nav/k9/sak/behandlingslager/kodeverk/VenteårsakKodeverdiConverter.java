package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;

@Converter(autoApply = true)
public class VenteårsakKodeverdiConverter implements AttributeConverter<Venteårsak, String> {
    @Override
    public String convertToDatabaseColumn(Venteårsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Venteårsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Venteårsak.fraKode(dbData);
    }
}
