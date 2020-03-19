package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@Converter(autoApply = true)
public class InntektskategoriKodeverdiConverter implements AttributeConverter<Inntektskategori, String> {
    @Override
    public String convertToDatabaseColumn(Inntektskategori attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Inntektskategori convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Inntektskategori.fraKode(dbData);
    }

}