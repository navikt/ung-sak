package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.vilkår.Utfall;

@Converter(autoApply = true)
public class UtfallKodeverdiConverter implements AttributeConverter<Utfall, String> {
    @Override
    public String convertToDatabaseColumn(Utfall attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Utfall convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Utfall.fraKode(dbData);
    }
}
