package no.nav.foreldrepenger.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.uttak.IkkeOppfyltÅrsak;

@Converter(autoApply = true)
public class UttakIkkeOppfyltKodeverdiConverter implements AttributeConverter<IkkeOppfyltÅrsak, String> {
    @Override
    public String convertToDatabaseColumn(IkkeOppfyltÅrsak attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public IkkeOppfyltÅrsak convertToEntityAttribute(String dbData) {
        return dbData == null ? null : IkkeOppfyltÅrsak.fraKode(dbData);
    }
}