package no.nav.folketrygdloven.beregningsgrunnlag.modell.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagRegelType;

@Converter(autoApply = true)
public class BeregningsgrunnlagRegelTypeKodeverdiConverter implements AttributeConverter<BeregningsgrunnlagRegelType, String> {

    @Override
    public String convertToDatabaseColumn(BeregningsgrunnlagRegelType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BeregningsgrunnlagRegelType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BeregningsgrunnlagRegelType.fraKode(dbData);
    }
}