package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.uttak.PeriodeResultatType;

@Converter(autoApply = true)
public class UttakPeriodeResultatKodeverdiConverter implements AttributeConverter<PeriodeResultatType, String> {
    @Override
    public String convertToDatabaseColumn(PeriodeResultatType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public PeriodeResultatType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PeriodeResultatType.fraKode(dbData);
    }
}