package no.nav.k9.sak.behandlingslager.behandling.historikk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.historikk.HistorikkAktør;

@Converter(autoApply = true)
public class HistorikkAktørKodeverdiConverter implements AttributeConverter<HistorikkAktør, String> {
    @Override
    public String convertToDatabaseColumn(HistorikkAktør attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public HistorikkAktør convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HistorikkAktør.fraKode(dbData);
    }
}