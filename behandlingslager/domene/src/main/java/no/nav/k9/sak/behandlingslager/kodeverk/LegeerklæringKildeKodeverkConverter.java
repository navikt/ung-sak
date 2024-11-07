package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.medisinsk.LegeerklæringKilde;

@Converter(autoApply = true)
public class LegeerklæringKildeKodeverkConverter implements AttributeConverter<LegeerklæringKilde, String> {
    @Override
    public String convertToDatabaseColumn(LegeerklæringKilde attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public LegeerklæringKilde convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LegeerklæringKilde.fraKode(dbData);
    }
}
