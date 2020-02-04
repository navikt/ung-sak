package no.nav.folketrygdloven.beregningsgrunnlag.modell.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@Converter(autoApply = true)
public class FaktaOmBeregningTilfelleKodeverdiConverter implements AttributeConverter<FaktaOmBeregningTilfelle, String> {

    @Override
    public String convertToDatabaseColumn(FaktaOmBeregningTilfelle attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public FaktaOmBeregningTilfelle convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FaktaOmBeregningTilfelle.fraKode(dbData);
    }
}