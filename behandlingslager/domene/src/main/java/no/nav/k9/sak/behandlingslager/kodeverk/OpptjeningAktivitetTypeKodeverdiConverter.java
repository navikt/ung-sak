package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

@Converter(autoApply = true)
public class OpptjeningAktivitetTypeKodeverdiConverter implements AttributeConverter<OpptjeningAktivitetType, String> {
    @Override
    public String convertToDatabaseColumn(OpptjeningAktivitetType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public OpptjeningAktivitetType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OpptjeningAktivitetType.fraKode(dbData);
    }

}