package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.medlem.MedlemskapManuellVurderingType;

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
