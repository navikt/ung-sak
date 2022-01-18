package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import javax.persistence.AttributeConverter;

import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;

public class SykdomVurderingTypeConverter implements AttributeConverter<SykdomVurderingType, String> {
    @Override
    public String convertToDatabaseColumn(SykdomVurderingType type) {
        return type.getKode();
    }
    
    @Override
    public SykdomVurderingType convertToEntityAttribute(String kode) {
        for (SykdomVurderingType type : SykdomVurderingType.values()) {
            if (type.getKode().equals(kode)) {
                return type;
            }
        }
        return null;
    }
}