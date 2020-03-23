package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.dokument.DokumentTypeId;

@Converter(autoApply = true)
public class DokumentTypeIdKodeverdiConverter implements AttributeConverter<DokumentTypeId, String> {
    @Override
    public String convertToDatabaseColumn(DokumentTypeId attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public DokumentTypeId convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DokumentTypeId.fraKode(dbData);
    }
}