package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.Fagsystem;

@Converter(autoApply = true)
public class FagsystemKodeverkConverter implements AttributeConverter<Fagsystem, String> {
    @Override
    public String convertToDatabaseColumn(Fagsystem attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Fagsystem convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Fagsystem.fraKode(dbData);
    }
}
