package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.dokument.Brevkode;

@Converter(autoApply = true)
public class BrevkodeKodeverdiConverter implements AttributeConverter<Brevkode, String> {
    @Override
    public String convertToDatabaseColumn(Brevkode attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Brevkode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Brevkode.fraKode(dbData);
    }
}
