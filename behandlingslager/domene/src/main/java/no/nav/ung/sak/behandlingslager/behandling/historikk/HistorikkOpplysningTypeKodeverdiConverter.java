package no.nav.ung.sak.behandlingslager.behandling.historikk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.historikk.HistorikkOpplysningType;

@Converter(autoApply = true)
public class HistorikkOpplysningTypeKodeverdiConverter implements AttributeConverter<HistorikkOpplysningType, String> {
    @Override
    public String convertToDatabaseColumn(HistorikkOpplysningType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public HistorikkOpplysningType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HistorikkOpplysningType.fraKode(dbData);
    }
}
