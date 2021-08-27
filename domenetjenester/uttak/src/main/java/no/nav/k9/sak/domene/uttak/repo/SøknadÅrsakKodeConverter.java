package no.nav.k9.sak.domene.uttak.repo;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.uttak.SøknadÅrsak;

@Converter(autoApply = true)
public class SøknadÅrsakKodeConverter implements AttributeConverter<SøknadÅrsak, String> {
    @Override
    public String convertToDatabaseColumn(SøknadÅrsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public SøknadÅrsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SøknadÅrsak.fraKode(dbData);
    }
}
