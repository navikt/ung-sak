package no.nav.ung.sak.oppgave;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;

@Converter(autoApply = true)
public class OppgaveTypeKodeverdiConverter implements AttributeConverter<OppgaveType, String> {
    @Override
    public String convertToDatabaseColumn(OppgaveType attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public OppgaveType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OppgaveType.fraKode(dbData);
    }
}
