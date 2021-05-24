package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.hendelser.HendelseType;

@Converter(autoApply = true)
public class HendelseTypeKodeverdiConverter implements AttributeConverter<HendelseType, String> {
    @Override
    public String convertToDatabaseColumn(HendelseType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public HendelseType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HendelseType.fraKode(dbData);
    }

}
