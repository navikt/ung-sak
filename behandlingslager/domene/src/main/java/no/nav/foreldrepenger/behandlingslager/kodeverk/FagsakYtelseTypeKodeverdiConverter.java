package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

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