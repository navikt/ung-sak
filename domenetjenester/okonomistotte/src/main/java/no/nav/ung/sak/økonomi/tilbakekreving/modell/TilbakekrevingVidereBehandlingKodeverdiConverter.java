package no.nav.ung.sak.økonomi.tilbakekreving.modell;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;

@Converter(autoApply = true)
public class TilbakekrevingVidereBehandlingKodeverdiConverter implements AttributeConverter<TilbakekrevingVidereBehandling, String> {
    @Override
    public String convertToDatabaseColumn(TilbakekrevingVidereBehandling attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public TilbakekrevingVidereBehandling convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TilbakekrevingVidereBehandling.fraKode(dbData);
    }
}
