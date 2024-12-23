package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

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
