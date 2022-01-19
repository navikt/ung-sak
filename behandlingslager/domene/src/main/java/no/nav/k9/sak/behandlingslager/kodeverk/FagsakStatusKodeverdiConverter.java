package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.behandling.FagsakStatus;

@Converter(autoApply = true)
public class FagsakStatusKodeverdiConverter implements AttributeConverter<FagsakStatus, String> {
    @Override
    public String convertToDatabaseColumn(FagsakStatus attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public FagsakStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FagsakStatus.fraKode(dbData);
    }
}
