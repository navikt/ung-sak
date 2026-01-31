package no.nav.ung.sak.oppgave;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.sak.kontrakt.oppgaver.BekreftelseDTO;

/**
 * Converter for å serialisere BekreftelseDTO til/fra JSONB.
 */
@Converter
public class OppgaveBekreftelseConverter implements AttributeConverter<BekreftelseDTO, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(BekreftelseDTO attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kunne ikke serialisere BekreftelseDTO til JSON", e);
        }
    }

    @Override
    public BekreftelseDTO convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, BekreftelseDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kunne ikke deserialisere BekreftelseDTO fra JSON", e);
        }
    }
}

