package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;

@Converter(autoApply = true)
public class VurderingKodeverdiConverter implements AttributeConverter<Vurdering, String> {
    @Override
    public String convertToDatabaseColumn(Vurdering attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Vurdering convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Vurdering.fraKode(dbData);
    }
}
