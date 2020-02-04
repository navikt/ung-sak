package no.nav.folketrygdloven.beregningsgrunnlag.modell.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAktivitetHandlingType;

@Converter(autoApply = true)
public class BeregningAktivitetHandlingTypeKodeverdiConverter implements AttributeConverter<BeregningAktivitetHandlingType, String> {

    @Override
    public String convertToDatabaseColumn(BeregningAktivitetHandlingType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BeregningAktivitetHandlingType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BeregningAktivitetHandlingType.fraKode(dbData);
    }
}