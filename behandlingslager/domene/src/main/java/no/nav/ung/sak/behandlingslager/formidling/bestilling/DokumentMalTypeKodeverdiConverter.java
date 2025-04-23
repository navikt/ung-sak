package no.nav.ung.sak.behandlingslager.formidling.bestilling;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.dokument.DokumentMalType;

@Converter(autoApply = true)
public class DokumentMalTypeKodeverdiConverter implements AttributeConverter<DokumentMalType, String> {
    @Override
    public String convertToDatabaseColumn(DokumentMalType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public DokumentMalType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DokumentMalType.fraKode(dbData);
    }
}
