package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.geografisk.Språkkode;

@Converter(autoApply = true)
public class SpråkKodeverdiConverter implements AttributeConverter<Språkkode, String> {
    @Override
    public String convertToDatabaseColumn(Språkkode attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Språkkode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Språkkode.fraKode(dbData);
    }
}