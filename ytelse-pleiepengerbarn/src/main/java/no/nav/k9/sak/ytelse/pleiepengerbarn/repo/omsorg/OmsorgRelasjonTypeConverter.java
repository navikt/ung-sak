package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

import jakarta.persistence.AttributeConverter;

import no.nav.k9.sak.kontrakt.omsorg.BarnRelasjon;

public class OmsorgRelasjonTypeConverter implements AttributeConverter<BarnRelasjon, String>{

    @Override
    public String convertToDatabaseColumn(BarnRelasjon relasjon) {
        return relasjon != null ? relasjon.getRolle() : null;
    }

    @Override
    public BarnRelasjon convertToEntityAttribute(String rolle) {
        for (BarnRelasjon relasjon : BarnRelasjon.values()) {
            if (relasjon.getRolle().equals(rolle)) {
                return relasjon;
            }
        }
        return null;
    }
}

