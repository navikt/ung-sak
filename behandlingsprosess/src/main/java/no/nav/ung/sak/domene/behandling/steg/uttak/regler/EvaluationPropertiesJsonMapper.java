package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;

import java.util.Map;

public class EvaluationPropertiesJsonMapper {

    public static String mapToJson(Map<String, String> properties) {
        return JsonObjectMapper.toJson(properties, JsonMappingFeil.FACTORY::jsonMappingFeil);
    }

    interface JsonMappingFeil extends DeklarerteFeil {

        JsonMappingFeil FACTORY = FeilFactory.create(JsonMappingFeil.class);

        @TekniskFeil(feilkode = "UNG-34524", feilmelding = "JSON-mapping feil: %s", logLevel = LogLevel.WARN)
        Feil jsonMappingFeil(JsonProcessingException e);
    }

}
