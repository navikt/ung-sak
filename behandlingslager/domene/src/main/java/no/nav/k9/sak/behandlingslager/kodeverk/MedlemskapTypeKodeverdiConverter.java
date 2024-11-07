package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.medlem.MedlemskapType;

@Converter(autoApply = true)
public class MedlemskapTypeKodeverdiConverter implements AttributeConverter<MedlemskapType, String> {
    @Override
    public String convertToDatabaseColumn(MedlemskapType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public MedlemskapType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MedlemskapType.fraKode(dbData);
    }
}
