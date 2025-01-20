package no.nav.ung.domenetjenester.personhendelser;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;

public class HendelseMapper {

    static String toJson(Hendelse hendelse) {
        try {
            return JsonUtils.getObjectMapper().writeValueAsString(hendelse);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kunne ikke serialisere hendelse", e);
        }
    }

    static Hendelse fraJson(String hendelse) {
        try {
            return JsonUtils.getObjectMapper().readValue(hendelse, Hendelse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kunne ikke deserialisere hendelse", e);
        }
    }


}
