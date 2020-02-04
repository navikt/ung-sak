package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.uttak.InnvilgetÅrsak;

@Converter(autoApply = true)
public class UttakInnvilgetKodeverdiConverter implements AttributeConverter<InnvilgetÅrsak, String> {
    @Override
    public String convertToDatabaseColumn(InnvilgetÅrsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public InnvilgetÅrsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InnvilgetÅrsak.fraKode(dbData);
    }
}