package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.sak.behandlingslager.behandling.opptjening.ReferanseType;

@Converter(autoApply = true)
public class OpptjeningReferanseTypeKodeverdiConverter implements AttributeConverter<ReferanseType, String> {
    @Override
    public String convertToDatabaseColumn(ReferanseType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public ReferanseType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReferanseType.fraKode(dbData);
    }
}
