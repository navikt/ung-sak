package no.nav.foreldrepenger.domene.risikoklassifisering.modell;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.risikoklassifisering.Kontrollresultat;

@Converter(autoApply = true)
public class KontrollresultatKodeverdiConverter implements AttributeConverter<Kontrollresultat, String> {
    @Override
    public String convertToDatabaseColumn(Kontrollresultat attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Kontrollresultat convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Kontrollresultat.fraKode(dbData);
    }
}