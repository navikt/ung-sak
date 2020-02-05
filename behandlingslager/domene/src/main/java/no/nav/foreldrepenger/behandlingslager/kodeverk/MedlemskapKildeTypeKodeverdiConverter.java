package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.medlem.MedlemskapKildeType;

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