package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

@Converter(autoApply = true)
public class FagsakYtelseTypeKodeverdiConverter implements AttributeConverter<FagsakYtelseType, String> {
    @Override
    public String convertToDatabaseColumn(FagsakYtelseType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public FagsakYtelseType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FagsakYtelseType.fraKode(dbData);
    }
}
