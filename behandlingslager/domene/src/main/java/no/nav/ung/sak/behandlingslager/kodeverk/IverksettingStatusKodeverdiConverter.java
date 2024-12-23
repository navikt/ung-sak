package no.nav.ung.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.ung.kodeverk.vedtak.IverksettingStatus;

@Converter(autoApply = true)
public class IverksettingStatusKodeverdiConverter implements AttributeConverter<IverksettingStatus, String> {
    @Override
    public String convertToDatabaseColumn(IverksettingStatus attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public IverksettingStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : IverksettingStatus.fraKode(dbData);
    }
}
