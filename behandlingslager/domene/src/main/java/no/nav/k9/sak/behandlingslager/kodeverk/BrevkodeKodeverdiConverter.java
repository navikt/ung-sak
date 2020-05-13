package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.dokument.Brevkode;

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