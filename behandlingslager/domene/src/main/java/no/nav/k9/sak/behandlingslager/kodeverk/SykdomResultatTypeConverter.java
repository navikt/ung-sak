package no.nav.k9.sak.behandlingslager.kodeverk;

import jakarta.persistence.AttributeConverter;
import no.nav.k9.kodeverk.sykdom.Resultat;

public class SykdomResultatTypeConverter implements AttributeConverter<Resultat, String> {
    @Override
    public String convertToDatabaseColumn(Resultat resultat) {
        return resultat.getKode();
    }

    @Override
    public Resultat convertToEntityAttribute(String kode) {
        for (Resultat resultat : Resultat.values()) {
            if (resultat.getKode().equals(kode)) {
                return resultat;
            }
        }
        return null;
    }
}
