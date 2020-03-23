package no.nav.k9.sak.domene.risikoklassifisering.modell;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.risikoklassifisering.FaresignalVurdering;

@Converter(autoApply = true)
public class FaresignalKodeverdiConverter implements AttributeConverter<FaresignalVurdering, String> {
    @Override
    public String convertToDatabaseColumn(FaresignalVurdering attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public FaresignalVurdering convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FaresignalVurdering.fraKode(dbData);
    }
}