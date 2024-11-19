package no.nav.ung.sak.behandlingslager.behandling.historikk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;

@Converter(autoApply = true)
public class HistorikkinnslagTypeKodeverdiConverter implements AttributeConverter<HistorikkinnslagType, String> {
    @Override
    public String convertToDatabaseColumn(HistorikkinnslagType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public HistorikkinnslagType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HistorikkinnslagType.fraKode(dbData);
    }
}
