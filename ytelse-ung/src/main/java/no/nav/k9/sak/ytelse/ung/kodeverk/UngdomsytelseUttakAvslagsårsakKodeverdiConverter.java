package no.nav.k9.sak.ytelse.ung.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.k9.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

@Converter(autoApply = true)
public class UngdomsytelseUttakAvslagsårsakKodeverdiConverter implements AttributeConverter<UngdomsytelseUttakAvslagsårsak, String> {

    @Override
    public String convertToDatabaseColumn(UngdomsytelseUttakAvslagsårsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public UngdomsytelseUttakAvslagsårsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UngdomsytelseUttakAvslagsårsak.fraKode(dbData);
    }
}
