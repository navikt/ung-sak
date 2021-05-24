package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.hendelser.HåndtertStatusType;

@Converter(autoApply = true)
public class HåndtertStatusTypeKodeverdiConverter implements AttributeConverter<HåndtertStatusType, String> {
    @Override
    public String convertToDatabaseColumn(HåndtertStatusType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public HåndtertStatusType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HåndtertStatusType.fraKode(dbData);
    }

}
