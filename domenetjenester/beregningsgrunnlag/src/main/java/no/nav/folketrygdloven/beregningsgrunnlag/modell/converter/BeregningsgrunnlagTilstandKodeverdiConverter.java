package no.nav.folketrygdloven.beregningsgrunnlag.modell.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;

@Converter(autoApply = true)
public class BeregningsgrunnlagTilstandKodeverdiConverter implements AttributeConverter<BeregningsgrunnlagTilstand, String> {

    @Override
    public String convertToDatabaseColumn(BeregningsgrunnlagTilstand attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BeregningsgrunnlagTilstand convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BeregningsgrunnlagTilstand.fraKode(dbData);
    }
}