package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.medlem.MedlemskapKildeType;

@Converter(autoApply = true)
public class MedlemskapKildeTypeKodeverdiConverter implements AttributeConverter<MedlemskapKildeType, String> {
    @Override
    public String convertToDatabaseColumn(MedlemskapKildeType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public MedlemskapKildeType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MedlemskapKildeType.fraKode(dbData);
    }
}
