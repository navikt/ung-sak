package no.nav.k9.sak.behandlingslager.behandling.historikk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.historikk.HistorikkinnslagFeltType;

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