package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.ReaktiveringStatus;

@Converter(autoApply = true)
public class ReaktiveringStatusKodeverdiConverter implements AttributeConverter<ReaktiveringStatus, String> {
    @Override
    public String convertToDatabaseColumn(ReaktiveringStatus attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public ReaktiveringStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReaktiveringStatus.fraKode(dbData);
    }
}
