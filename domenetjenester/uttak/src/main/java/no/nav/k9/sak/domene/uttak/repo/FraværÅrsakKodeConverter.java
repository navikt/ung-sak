package no.nav.k9.sak.domene.uttak.repo;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;

@Converter(autoApply = true)
public class FraværÅrsakKodeConverter implements AttributeConverter<FraværÅrsak, String> {
    @Override
    public String convertToDatabaseColumn(FraværÅrsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public FraværÅrsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FraværÅrsak.fraKode(dbData);
    }
}
