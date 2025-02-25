package no.nav.ung.sak.behandlingslager.behandling.historikk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.historikk.HistorikkinnslagFeltType;

@Converter(autoApply = true)
public class HistorikkInnslagFeltTypeKodeverdiConverter implements AttributeConverter<HistorikkinnslagFeltType, String> {
    @Override
    public String convertToDatabaseColumn(HistorikkinnslagFeltType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public HistorikkinnslagFeltType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HistorikkinnslagFeltType.fraKode(dbData);
    }
}
