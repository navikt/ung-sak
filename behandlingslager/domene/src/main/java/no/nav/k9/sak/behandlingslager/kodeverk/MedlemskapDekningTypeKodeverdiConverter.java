package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;

@Converter(autoApply = true)
public class MedlemskapDekningTypeKodeverdiConverter implements AttributeConverter<MedlemskapDekningType, String> {
    @Override
    public String convertToDatabaseColumn(MedlemskapDekningType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public MedlemskapDekningType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MedlemskapDekningType.fraKode(dbData);
    }
}