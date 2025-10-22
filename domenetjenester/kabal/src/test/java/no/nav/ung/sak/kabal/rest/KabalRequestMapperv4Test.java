package no.nav.ung.sak.kabal.rest;

import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;
import no.nav.ung.sak.kabal.kontrakt.KabalRequestv4;
import no.nav.ung.sak.kabal.task.KabalRequestMapperV4;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.nav.ung.sak.kabal.rest.KabalRestKlient.objectMapper;

class KabalRequestMapperv4Test {

    private static Behandling klageBehandling;
    private static KabalRequestv4 kabalRequest;
    private static String jsonActual;

    @BeforeAll
    static void setup() {
        KabalRequestMapperV4 mapper = new KabalRequestMapperV4();
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
            "-", "-", null, "kabalref", KlageVurdertAv.VEDTAKSINSTANS);

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
        Assertions.assertEquals(klageBehandling.getBehandlendeEnhet(), kabalRequest.forrigeBehandlendeEnhet(), "forrigeBehandlendeEnhet");
        Assertions.assertEquals(klageBehandling.getUuid().toString(), kabalRequest.kildeReferanse(), "kildeReferanse");
        // Assertions.assertNotNull(kabalRequest.klager(), "klager");
        Assertions.assertNotNull(kabalRequest.sakenGjelder(), "sakenGjelder");
        Assertions.assertEquals("KLAGE", kabalRequest.type(), "type");
        Assertions.assertEquals(klageBehandling.getUuid().toString(), kabalRequest.kildeReferanse());
        // MA, KS, AO mappes om til OMP i KabalRequestMapper.
        Assertions.assertEquals(klageBehandling.getFagsakYtelseType().getKode(), FagsakYtelseType.UNGDOMSYTELSE.getKode());
    }

    @Test
    void json_mapping_test() {
        String expectedJson = """
            {
               "type":"KLAGE",
               "sakenGjelder":{
                  "id":{
                     "type":"PERSON",
                     "verdi":null
                  }
               },
               "fagsak": {
                  "fagsakId": "saksnummer",
                  "fagsystem": "UNG_SAK"
               },
               "kildeReferanse":"behandlingUuid",
               "hjemler": [
               ],
               "forrigeBehandlendeEnhet":"4401",
               "tilknyttedeJournalposter":[
               ],
               "ytelse": "UNG_UNG"
            }
            """;

        expectedJson = expectedJson
            .replaceAll("behandlingUuid", klageBehandling.getUuid().toString())
            .replaceAll("saksnummer", klageBehandling.getFagsak().getSaksnummer().getVerdi())
            .replaceAll(" ", "")
            .replaceAll("\n", "");

        Assertions.assertEquals(expectedJson, jsonActual.trim());
    }
}
