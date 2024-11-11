package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.person.RelasjonsRolleType;

@Converter(autoApply = true)
public class RelasjonsRolleTypeKodeverdiConverter implements AttributeConverter<RelasjonsRolleType, String> {
    @Override
    public String convertToDatabaseColumn(RelasjonsRolleType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public RelasjonsRolleType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RelasjonsRolleType.fraKode(dbData);
    }
}
