package no.nav.k9.sak.behandlingslager.kodeverk;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.k9.kodeverk.dokument.DokumentKategori;

@Converter(autoApply = true)
public class DokumentKategoriKodeverdiConverter implements AttributeConverter<DokumentKategori, String> {
    @Override
    public String convertToDatabaseColumn(DokumentKategori attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public DokumentKategori convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DokumentKategori.fraKode(dbData);
    }
}