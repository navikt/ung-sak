package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.organisasjon.Organisasjonstype;

@Converter(autoApply = true)
public class OrganisasjonstypeKodeverdiConverter implements AttributeConverter<Organisasjonstype, String> {
    @Override
    public String convertToDatabaseColumn(Organisasjonstype attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Organisasjonstype convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Organisasjonstype.fraKode(dbData);
    }
}
