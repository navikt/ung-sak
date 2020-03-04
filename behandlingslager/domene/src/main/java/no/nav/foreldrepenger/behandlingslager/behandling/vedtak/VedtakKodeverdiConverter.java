package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;

@Converter(autoApply = true)
public class VedtakKodeverdiConverter implements AttributeConverter<Vedtaksbrev, String> {
    @Override
    public String convertToDatabaseColumn(Vedtaksbrev attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Vedtaksbrev convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Vedtaksbrev.fraKode(dbData);
    }
}