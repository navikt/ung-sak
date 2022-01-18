package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;

@Converter(autoApply = true)
public class AktivitetStatusKodeverdiConverter implements AttributeConverter<AktivitetStatus, String> {

    @Override
    public String convertToDatabaseColumn(AktivitetStatus attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public AktivitetStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AktivitetStatus.fraKode(dbData);
    }
}