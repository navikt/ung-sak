package no.nav.ung.sak.behandlingslager.behandling.historikk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.historikk.HistorikkResultatType;

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
