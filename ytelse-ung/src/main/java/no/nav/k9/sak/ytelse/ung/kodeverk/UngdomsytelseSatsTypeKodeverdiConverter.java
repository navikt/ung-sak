package no.nav.k9.sak.ytelse.ung.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.k9.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.k9.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

@Converter(autoApply = true)
public class UngdomsytelseSatsTypeKodeverdiConverter implements AttributeConverter<UngdomsytelseSatsType, String> {

    @Override
    public String convertToDatabaseColumn(UngdomsytelseSatsType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public UngdomsytelseSatsType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UngdomsytelseSatsType.fraKode(dbData);
    }
}
