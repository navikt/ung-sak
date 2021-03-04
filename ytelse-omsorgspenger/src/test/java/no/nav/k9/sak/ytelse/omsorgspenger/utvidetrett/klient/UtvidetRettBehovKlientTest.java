package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.behov.BehovKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.*;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UtvidetRettBehovKlientTest {

    private TestBehovKlient testBehovKlient = new TestBehovKlient();
    private UtvidetRettBehovKlient klient = new UtvidetRettBehovKlient(testBehovKlient);

    @Test
    public void innvilget_kronisk_sykt_barn_med_identitesnummer() {
        klient.innvilget(kroniskSyktBarnMedIdentitetsnummer.utvidetRett);
        assertEquals("InnvilgetKroniskSyktBarn", testBehovKlient.forrigeBehovNavn);
        assertEquals(kroniskSyktBarnMedIdentitetsnummer.forventetSerialisert, testBehovKlient.forrigeBehovInput);
    }

    @Test
    public void avslått_kronisk_sykt_barn_med_identitesnummer() {
        klient.avslått(kroniskSyktBarnMedIdentitetsnummer.utvidetRett);
        assertEquals("AvslåttKroniskSyktBarn", testBehovKlient.forrigeBehovNavn);
        assertEquals(kroniskSyktBarnMedIdentitetsnummer.forventetSerialisert, testBehovKlient.forrigeBehovInput);
    }

    @Test
    public void innvilget_midlertidig_alene() {
        klient.innvilget(midlertidigAlene.utvidetRett);
        assertEquals("InnvilgetMidlertidigAlene", testBehovKlient.forrigeBehovNavn);
        assertEquals(midlertidigAlene.forventetSerialisert, testBehovKlient.forrigeBehovInput);
    }

    @Test
    public void avslått_midlertidig_alene() {
        klient.avslått(midlertidigAlene.utvidetRett);
        assertEquals("AvslåttMidlertidigAlene", testBehovKlient.forrigeBehovNavn);
        assertEquals(midlertidigAlene.forventetSerialisert, testBehovKlient.forrigeBehovInput);
    }

    @Test
    public void vedtak_med_valideringsfeil() {
        var utvidetRett = new MidlertidigAlene(
            new Saksnummer("ENSAK125"),
            UUID.fromString("5dc41c02-7148-11eb-9439-0242ac130002"),
            ZonedDateTime.parse("2021-02-17T15:57:00.684Z"),
            ZonedDateTime.parse("2021-02-17T15:57:00.684+02"),
            new Person(new AktørId("29099011111")),
            null
        );
        assertThrows(IllegalStateException.class, () -> klient.avslått(utvidetRett));
        assertThrows(IllegalStateException.class, () -> klient.innvilget(utvidetRett));
    }

    @Test
    public void ikke_støttet_vedtak() {
        var utvidetRett = new TestUtvidetRett();
        assertThrows(IllegalStateException.class, () -> klient.innvilget(utvidetRett));
        assertThrows(IllegalStateException.class, () -> klient.avslått(utvidetRett));
    }

    private static UtvidetRettSerialisering kroniskSyktBarnMedIdentitetsnummer = new UtvidetRettSerialisering<>(
        new KroniskSyktBarn(
            new Saksnummer("ENSAK123"),
            UUID.fromString("0a98ac74-6970-47a5-8b0b-a14ead63082a"),
            ZonedDateTime.parse("2021-02-17T15:57:00.684Z"),
            ZonedDateTime.parse("2021-02-17T15:57:00.684+02"),
            new Person(new AktørId("29099011111")),
            new Person(new AktørId("01011811111"))
        ),
        "{\"saksnummer\":\"ENSAK123\",\"behandlingId\":\"0a98ac74-6970-47a5-8b0b-a14ead63082a\",\"søknadMottatt\":\"2021-02-17T15:57:00.684Z\",\"tidspunkt\":\"2021-02-17T13:57:00.684Z\",\"søker\":{\"aktørId\":\"29099011111\"},\"barn\":{\"aktørId\":\"01011811111\"}}"
    );

    private static UtvidetRettSerialisering midlertidigAlene = new UtvidetRettSerialisering<>(
        new MidlertidigAlene(
            new Saksnummer("ENSAK124"),
            UUID.fromString("b684c176-7147-11eb-9439-0242ac130002"),
            ZonedDateTime.parse("2021-02-17T15:57:00.684Z"),
            ZonedDateTime.parse("2021-02-17T15:57:00.684+02"),
            new Person(new AktørId("29099011111")),
            new Person(new AktørId("01011811111"))
        ),
        "{\"saksnummer\":\"ENSAK124\",\"behandlingId\":\"b684c176-7147-11eb-9439-0242ac130002\",\"søknadMottatt\":\"2021-02-17T15:57:00.684Z\",\"tidspunkt\":\"2021-02-17T13:57:00.684Z\",\"søker\":{\"aktørId\":\"29099011111\"},\"annenForelder\":{\"aktørId\":\"01011811111\"}}"
    );

    private static class TestBehovKlient extends BehovKlient {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        public String forrigeBehovNavn;
        public String forrigeBehovInput;

        @Override
        public void send(String behovssekvensId, String behovssekvens) {
            try {
                var forrigeBehovssekvens = (ObjectNode) OBJECT_MAPPER.readTree(behovssekvens);
                forrigeBehovNavn = forrigeBehovssekvens.get("@behov").fieldNames().next();
                forrigeBehovInput = forrigeBehovssekvens.get("@behov").get(forrigeBehovNavn).toString();
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Feil ved deserialisering av " + behovssekvens);
            }
        }
    }

    private static class TestUtvidetRett implements UtvidetRett {}

    private static class UtvidetRettSerialisering<V extends UtvidetRett> {
        public final V utvidetRett;
        public final String forventetSerialisert;

        private UtvidetRettSerialisering(V utvidetRett, String forventetSerialisert) {
            this.utvidetRett = utvidetRett;
            this.forventetSerialisert = forventetSerialisert;
        }
    }
}
