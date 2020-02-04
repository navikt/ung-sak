package no.nav.folketrygdloven.beregningsgrunnlag.modell.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.beregningsgrunnlag.SammenligningsgrunnlagType;

@Converter(autoApply = true)
public class SammenlignigsgrunnlagTypeKodeverdiConverter implements AttributeConverter<SammenligningsgrunnlagType, String> {
    @Override
    public String convertToDatabaseColumn(SammenligningsgrunnlagType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public SammenligningsgrunnlagType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SammenligningsgrunnlagType.fraKode(dbData);
    }
}