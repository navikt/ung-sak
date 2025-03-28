package no.nav.ung.domenetjenester.arkiv;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import no.nav.k9.søknad.JsonUtils;

public enum Søknadsformat {
    NY,
    OPPGAVEBEKREFTELSE;

    public static Optional<SøknadPayload> inspiserPayload(String payload, boolean dumpVedFeil) {
        boolean jsonGuess = VurderStrukturertDokumentTask.erJson(payload);
        if (jsonGuess) {
            JsonNode jsonNode;
            try {
                jsonNode = JsonUtils.getObjectMapper().readTree(payload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Parsing av melding feilet", e);
            }

            if (jsonNode.hasNonNull("data")) {
                // kafka topic format - delvis unwrapped
                jsonNode = jsonNode.get("data");
            } // else ta formatet som det er

            JsonNode søknad;
            if (jsonNode.hasNonNull("søknad")) {
                søknad = jsonNode.get("søknad");
            } else if (jsonNode.hasNonNull("søknadId")) {
                søknad = jsonNode;
            } else {
                throw new IllegalArgumentException("har ikke forventet format " + dump(payload, dumpVedFeil));
            }

            boolean erForventetSøknad = søknad.has("søknadId") && (søknad.hasNonNull("mottattDato") || søknad.hasNonNull("mottatt"));
            if (erForventetSøknad) {
                var søknadsformat = getSøknadsformat(søknad);
                return Optional.of(new SøknadPayload(søknad, søknadsformat));
            } else {
                throw new IllegalArgumentException("Ukjent " + dump(payload, dumpVedFeil));
            }

        }
        return Optional.empty();
    }

    private static Søknadsformat getSøknadsformat(JsonNode søknad) {
        final var ytelse = søknad.get("ytelse");
        final boolean harYtelseType = ytelse != null && ytelse.hasNonNull("type");
        if (harYtelseType) {
            return Søknadsformat.NY;
        }
        return Søknadsformat.OPPGAVEBEKREFTELSE;
    }

    private static String dump(String payload, boolean dumpVedFeil) {
        return dumpVedFeil ? "payload: " + payload : "payload: " + payload.substring(0, Math.min(15, payload.length()));
    }
}
