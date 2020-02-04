package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;

@Converter(autoApply = true)
public class UttakKodeverdiConverter implements AttributeConverter<UttakArbeidType, String> {
    @Override
    public String convertToDatabaseColumn(UttakArbeidType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public UttakArbeidType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UttakArbeidType.fraKode(dbData);
    }
}