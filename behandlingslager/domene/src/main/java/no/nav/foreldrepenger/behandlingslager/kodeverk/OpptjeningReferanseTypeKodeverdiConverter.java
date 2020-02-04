package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.ReferanseType;

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