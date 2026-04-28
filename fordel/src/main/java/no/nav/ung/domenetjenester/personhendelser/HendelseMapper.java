package no.nav.ung.domenetjenester.personhendelser;

import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;

public class HendelseMapper {

    static String toJson(Hendelse hendelse) {
        try {
            return JsonUtils.toString(hendelse);
        } catch (Exception e) {
            throw new IllegalStateException("Kunne ikke serialisere hendelse", e);
        }
    }

    static Hendelse fraJson(String hendelse) {
        try {
            return JsonUtils.fromString(hendelse, Hendelse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Kunne ikke deserialisere hendelse", e);
        }
    }


}
