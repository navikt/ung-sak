package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.geografisk.Landkoder;

@Converter(autoApply = true)
public class LandkoderKodeverdiConverter implements AttributeConverter<Landkoder, String> {
    @Override
    public String convertToDatabaseColumn(Landkoder attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Landkoder convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Landkoder.fraKode(dbData);
    }
}
