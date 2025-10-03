package no.nav.ung.sak.kabal.rest;

import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;
import no.nav.ung.sak.kabal.kontrakt.KabalRequest;
import no.nav.ung.sak.kabal.task.KabalRequestMapper;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.PersonIdent;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.time.LocalDate;

import static no.nav.ung.sak.kabal.rest.KabalRestKlient.objectMapper;

class KabalMappingTest {

    private static Behandling klageBehandling;
    private static KabalRequest kabalRequest;
    private static String jsonActual;

    @BeforeAll
    static void setup() {
        KabalRequestMapper mapper = new KabalRequestMapper();
        klageBehandling = TestScenarioBuilder
            .builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE)
            .medBehandlingType(BehandlingType.KLAGE)
            .lagMocked();
        var personIdent = new PersonIdent("29028535056");

        var klageUtredning = KlageUtredningEntitet.builder()
            .medKlageBehandling(klageBehandling)
            .medOpprinneligBehandlendeEnhet("4401")
            .medId(1L)
            .build();

        var klageVurderingAdapter = new KlageVurderingAdapter(
            KlageVurderingType.STADFESTE_YTELSESVEDTAK, KlageMedholdÅrsak.UDEFINERT, KlageVurderingOmgjør.UDEFINERT,
            "-", "-", Hjemmel.UNG_FORSKRIFT_PARAGRAF_11, "kabalref", KlageVurdertAv.VEDTAKSINSTANS);

        klageUtredning.setKlagevurdering(klageVurderingAdapter);

        klageBehandling.setBehandlendeEnhet(new OrganisasjonsEnhet("4401", "Testenhet"));
        klageBehandling.setAnsvarligSaksbehandler("Z1234567");
        klageBehandling.setMigrertKilde(Fagsystem.UNG_SAK);
        kabalRequest = mapper.map(klageBehandling, personIdent, klageUtredning);

        try {
            jsonActual = objectMapper.writer().writeValueAsString(kabalRequest);
        } catch (Exception e) {
            Assertions.assertNotNull(jsonActual, e.getMessage());
        }
    }

    @Test
    void kabal_request_mapper_test() {
        Assertions.assertEquals(klageBehandling.getBehandlendeEnhet(), kabalRequest.getAvsenderEnhet(), "getAvsenderEnhet");
        Assertions.assertEquals(klageBehandling.getAnsvarligSaksbehandler(), kabalRequest.getAvsenderSaksbehandlerIdent(), "getAvsenderSaksbehandlerIdent");
        Assertions.assertEquals(klageBehandling.getOpprettetDato().toLocalDate().toString(), kabalRequest.getInnsendtTilNav(), "getInnsendtTilNav");
        Assertions.assertEquals("UNG", kabalRequest.getKilde(), "getKilde");
        Assertions.assertEquals(klageBehandling.getUuid().toString(), kabalRequest.getKildeReferanse(), "getKildeReferanse");
        Assertions.assertNotNull(kabalRequest.getKlager(), "getKlager");
        Assertions.assertEquals("KLAGE", kabalRequest.getType(), "getType");
        Assertions.assertEquals(klageBehandling.getUuid().toString(), kabalRequest.getDvhReferanse());
        // MA, KS, AO mappes om til OMP i KabalRequestMapper.
        Assertions.assertEquals(klageBehandling.getFagsakYtelseType().getKode(), FagsakYtelseType.UNGDOMSYTELSE.getKode());
    }

    @Test
    void json_mapping_test() throws JSONException {
        String expectedJson = """
            {
               "avsenderEnhet":"4401",
               "avsenderSaksbehandlerIdent":"Z1234567",
               "innsendtTilNav":"dagensDato",
               "kilde":"UNG",
               "kildeReferanse":"behandlingUuid",
               "klager":{
                  "id":{
                     "type":"PERSON",
                     "verdi":null
                  },
                  "klagersProsessfullmektig":null
               },
               "mottattFoersteinstans":"dagensDato",
               "tilknyttedeJournalposter":[
               ],
               "type":"KLAGE",
               "hjemler": [
                "UNG_FRSKRFT_11"
               ],
               "dvhReferanse":"behandlingUuid",
               "ytelse": "UNG",
               "oversendtKaDato": "nå",
               "fagsak": {
                  "fagsakId": "saksnummer",
                  "fagsystem": "UNG_SAK"
               }
            }
            """;

        expectedJson = expectedJson
            .replaceAll("dagensDato", LocalDate.now().toString())
            .replaceAll("behandlingUuid", klageBehandling.getUuid().toString())
            .replaceAll("nå", kabalRequest.getOversendtKaDato())
            .replaceAll("saksnummer", klageBehandling.getFagsak().getSaksnummer().getVerdi());

        JSONAssert.assertEquals(expectedJson, jsonActual, true);
    }
}
