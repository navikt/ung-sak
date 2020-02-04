package no.nav.folketrygdloven.beregningsgrunnlag.modell.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagPeriodeRegelType;

@Converter(autoApply = true)
public class BeregningsgrunnlagPeriodeRegelTypeKodeverdiConverter implements AttributeConverter<BeregningsgrunnlagPeriodeRegelType, String> {

    @Override
    public String convertToDatabaseColumn(BeregningsgrunnlagPeriodeRegelType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BeregningsgrunnlagPeriodeRegelType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BeregningsgrunnlagPeriodeRegelType.fraKode(dbData);
    }
}