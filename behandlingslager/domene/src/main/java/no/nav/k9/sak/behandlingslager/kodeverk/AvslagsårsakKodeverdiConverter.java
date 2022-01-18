package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;

@Converter(autoApply = true)
public class AvslagsårsakKodeverdiConverter implements AttributeConverter<Avslagsårsak, String> {
    @Override
    public String convertToDatabaseColumn(Avslagsårsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Avslagsårsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Avslagsårsak.fraKode(dbData);
    }
}
