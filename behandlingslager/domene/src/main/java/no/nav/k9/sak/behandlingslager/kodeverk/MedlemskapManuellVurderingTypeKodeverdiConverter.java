package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;

@Converter(autoApply = true)
public class MedlemskapManuellVurderingTypeKodeverdiConverter implements AttributeConverter<MedlemskapManuellVurderingType, String> {
    @Override
    public String convertToDatabaseColumn(MedlemskapManuellVurderingType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public MedlemskapManuellVurderingType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MedlemskapManuellVurderingType.fraKode(dbData);
    }
}