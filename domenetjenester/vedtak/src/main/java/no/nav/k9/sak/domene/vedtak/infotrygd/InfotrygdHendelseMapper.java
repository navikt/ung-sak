package no.nav.k9.sak.domene.vedtak.infotrygd;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedElement;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

class InfotrygdHendelseMapper {

    InfotrygdHendelseMapper() {
    }

    InfotrygdHendelse mapFraFeedTilInfotrygdHendelse(FeedElement feedElement) {
        String payload = JsonMapper.toJson(feedElement.getInnhold());

        Optional<Meldingstype> meldingstype = Arrays.stream(Meldingstype.values()).filter(e -> e.getType().equals(feedElement.getType())).findFirst();

        if (meldingstype.isEmpty()) {
            return InfotrygdHendelse.builder().build();
        }
        Innhold innhold = JsonMapper.fromJson(payload, meldingstype.get().getMeldingsDto());

        return InfotrygdHendelse.builder()
            .medSekvensnummer(feedElement.getSekvensId())
            .medType(feedElement.getType())
            .medAktørId(Long.valueOf(innhold.getAktoerId()))
            .medTypeYtelse(innhold.getTypeYtelse())
            .medFom(innhold.getFom())
            .medIdentDato(innhold.getIdentDato())
            .build();
    }

    private static class JsonMapper {

        private static final ObjectMapper MAPPER = getObjectMapper();

        private static ObjectMapper getObjectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper;
        }

        private static <T> T fromJson(String json, Class<T> clazz) {
            try {
                return MAPPER.readerFor(clazz).readValue(json);
            } catch (IOException e) {
                throw JsonMapperFeil.FACTORY.ioExceptionVedLesing(e).toException();
            }
        }

        private static String toJson(Object dto) {
            try {
                return MAPPER.writeValueAsString(dto);
            } catch (JsonProcessingException e) {
                throw JsonMapperFeil.FACTORY.kunneIkkeSerialisereJson(e).toException();
            }
        }


        interface JsonMapperFeil extends DeklarerteFeil {

            static final JsonMapperFeil FACTORY = FeilFactory.create(JsonMapperFeil.class);

            @TekniskFeil(feilkode = "F-728314", feilmelding = "Kunne ikke serialisere objekt til JSON", logLevel = LogLevel.WARN)
            Feil kunneIkkeSerialisereJson(JsonProcessingException cause);

            @TekniskFeil(feilkode = "F-723328", feilmelding = "Fikk IO exception ved parsing av JSON", logLevel = LogLevel.WARN)
            Feil ioExceptionVedLesing(IOException cause);

        }

    }


}
