package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;

@Converter(autoApply = true)
public class UtenlandsoppholdÅrsakConverter implements AttributeConverter<UtenlandsoppholdÅrsak, String> {
    @Override
    public String convertToDatabaseColumn(UtenlandsoppholdÅrsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public UtenlandsoppholdÅrsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UtenlandsoppholdÅrsak.fraKode(dbData);
    }
}
