package no.nav.k9.sak.behandlingslager.behandling.historikk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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