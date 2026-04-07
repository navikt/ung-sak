package no.nav.ung.sak.behandlingslager.behandling;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.ung.kodeverk.behandling.BehandlingDel;

import java.util.Arrays;

@Converter(autoApply = true)
public class BehandlingDelKodeverdiConverter implements AttributeConverter<BehandlingDel, String> {
    @Override
    public String convertToDatabaseColumn(BehandlingDel attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public BehandlingDel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Arrays.stream(BehandlingDel.values()).filter(it -> it.getKode().equals(dbData)).findFirst().orElseThrow();
    }
}
