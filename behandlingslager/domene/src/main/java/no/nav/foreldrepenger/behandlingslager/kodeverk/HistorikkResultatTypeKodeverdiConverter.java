package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.historikk.HistorikkResultatType;

@Converter(autoApply = true)
public class HistorikkResultatTypeKodeverdiConverter implements AttributeConverter<HistorikkResultatType, String> {
    @Override
    public String convertToDatabaseColumn(HistorikkResultatType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public HistorikkResultatType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HistorikkResultatType.fraKode(dbData);
    }
}