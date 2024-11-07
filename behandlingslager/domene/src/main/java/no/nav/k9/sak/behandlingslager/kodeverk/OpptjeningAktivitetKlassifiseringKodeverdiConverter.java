package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;

@Converter(autoApply = true)
public class OpptjeningAktivitetKlassifiseringKodeverdiConverter implements AttributeConverter<OpptjeningAktivitetKlassifisering, String> {
    @Override
    public String convertToDatabaseColumn(OpptjeningAktivitetKlassifisering attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public OpptjeningAktivitetKlassifisering convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OpptjeningAktivitetKlassifisering.fraKode(dbData);
    }
}
