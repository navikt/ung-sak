package no.nav.ung.sak.oppgave;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

@Converter(autoApply = true)
public class OppgaveTypeKodeverdiConverter implements AttributeConverter<AksjonspunktDefinisjon, String> {
    @Override
    public String convertToDatabaseColumn(AksjonspunktDefinisjon attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public AksjonspunktDefinisjon convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AksjonspunktDefinisjon.fraKode(dbData);
    }
}
