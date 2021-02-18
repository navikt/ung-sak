package no.nav.k9.sak.ytelse.omsorgspenger.behov;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.huxhorn.sulky.ulid.ULID;
import no.nav.k9.rapid.behov.Behov;
import no.nav.k9.rapid.behov.Behovssekvens;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.Json;
import no.nav.vedtak.log.mdc.MDCOperations;

import java.util.Map;
import java.util.Optional;

public abstract class BehovKlient {
    private static final ObjectMapper OBJECT_MAPPER = Json.getObjectMapper();
    private static final ULID ulid = new ULID();

    private static String correlationId() {
        return Optional.of(MDCOperations.generateCallId()).orElse(MDCOperations.generateCallId());
    }

    private void nyttBehov(Behov behov) {
        var behovssekvens = new Behovssekvens(ulid.nextULID(), correlationId(), behov).getKeyValue();
        send(behovssekvens.getFirst(), behovssekvens.getSecond());
    }

    public void nyttBehov(String navn, Object input) {
        nyttBehov(new Behov(navn, OBJECT_MAPPER.convertValue(input, new TypeReference<Map<String, Object>>() {})));
    }

    public abstract void send(String behovssekvensId, String behovssekvens);
}
