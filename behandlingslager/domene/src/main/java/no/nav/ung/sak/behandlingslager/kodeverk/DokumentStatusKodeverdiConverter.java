package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.dokument.DokumentStatus;

@Converter(autoApply = true)
public class DokumentStatusKodeverdiConverter implements AttributeConverter<DokumentStatus, String> {
    private static final DokumentStatus NULL_VALUE = DokumentStatus.GYLDIG;

    @Override
    public String convertToDatabaseColumn(DokumentStatus attribute) {
        return attribute == null ? NULL_VALUE.getKode() : attribute.getKode();
    }

    @Override
    public DokumentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? NULL_VALUE : DokumentStatus.fraKode(dbData);
    }
}
