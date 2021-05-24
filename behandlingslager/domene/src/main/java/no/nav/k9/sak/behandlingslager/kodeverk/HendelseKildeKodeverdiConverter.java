package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.hendelser.HendelseKilde;

@Converter(autoApply = true)
public class HendelseKildeKodeverdiConverter implements AttributeConverter<HendelseKilde, String> {
    @Override
    public String convertToDatabaseColumn(HendelseKilde attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public HendelseKilde convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HendelseKilde.fraKode(dbData);
    }

}
