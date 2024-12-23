package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.opptjening.OpptjeningAktivitetType;

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
