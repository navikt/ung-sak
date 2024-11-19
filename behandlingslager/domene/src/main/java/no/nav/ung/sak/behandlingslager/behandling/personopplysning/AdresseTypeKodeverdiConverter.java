package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.geografisk.AdresseType;

@Converter(autoApply = true)
public class AdresseTypeKodeverdiConverter implements AttributeConverter<AdresseType, String> {
    @Override
    public String convertToDatabaseColumn(AdresseType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public AdresseType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AdresseType.fraKode(dbData);
    }

}
