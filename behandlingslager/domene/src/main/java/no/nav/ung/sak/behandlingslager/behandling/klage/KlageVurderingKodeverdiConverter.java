package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.klage.KlageVurdering;

@Converter(autoApply = true)
public class KlageVurderingKodeverdiConverter implements AttributeConverter<KlageVurdering, String> {
    @Override
    public String convertToDatabaseColumn(KlageVurdering attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public KlageVurdering convertToEntityAttribute(String dbData) {
        return dbData == null ? null : KlageVurdering.fraKode(dbData);
    }
}
