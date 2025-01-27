package no.nav.ung.sak.tilgangskontroll;

import no.nav.k9.felles.sikkerhet.abac.Decision;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.ung.abac.BeskyttetRessursKoder;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.ung.sak.tilgangskontroll.api.AbacAttributter;
import no.nav.ung.sak.tilgangskontroll.api.AbacBehandlingStatus;
import no.nav.ung.sak.tilgangskontroll.api.AbacFagsakStatus;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.PersonPipRestKlient;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto.*;
import no.nav.ung.sak.tilgangskontroll.integrasjon.skjermetperson.SkjermetPersonRestKlient;
import no.nav.ung.sak.tilgangskontroll.tilganger.*;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDecisionPointTest {

    public static final String AKTØR_ID = "1234567890123";
    public static final String FNR = "12345678901";
    private AnsattTilgangerTjeneste ansattTilgangerTjeneste = Mockito.mock(AnsattTilgangerTjeneste.class);
    private PersonPipRestKlient personPipRestKlient = Mockito.mock(PersonPipRestKlient.class);
    private SkjermetPersonRestKlient skjermetPersonRestKlient = Mockito.mock(SkjermetPersonRestKlient.class);
    private PersonDiskresjonskodeTjeneste personDiskresjonskodeTjeneste = new PersonDiskresjonskodeTjeneste(personPipRestKlient, skjermetPersonRestKlient);
    private TilgangTilPersonTjeneste tilgangTilPersonTjeneste = new TilgangTilPersonTjeneste(personDiskresjonskodeTjeneste);
    private TilgangTilOperasjonTjeneste tilgangTilOperasjonTjeneste = new TilgangTilOperasjonTjeneste();

    private PolicyDecisionPoint pdp = new PolicyDecisionPoint(ansattTilgangerTjeneste, tilgangTilPersonTjeneste, tilgangTilOperasjonTjeneste);

    void mockPerson(PersonIdent personIdent, AktørId aktørId, boolean erSkjermet, Set<AdressebeskyttelseGradering> adressebeskyttelser) {
        Mockito.when(personPipRestKlient.hentAdressebeskyttelse(Mockito.eq(personIdent))).thenReturn(adressebeskyttelser);
        Mockito.when(personPipRestKlient.hentPersoninformasjon(Mockito.eq(aktørId))).thenReturn(new PipPersondataResponse(AKTØR_ID, new PipPerson(adressebeskyttelser.stream().map(Adressebeskyttelse::fraEnum).toArray(Adressebeskyttelse[]::new)), new PipIdenter(List.of(new PipIdent(FNR, false, "FOLKEREGISTERIDENT")))));
        Mockito.when(skjermetPersonRestKlient.erPersonSkjermet(Mockito.eq(personIdent))).thenReturn(erSkjermet);
    }

    @Test
    void veileder_skal_ha_tilgang_til_å_lese_fagsak_med_bruker_uten_beskyttelsesbehov() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of());
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "read");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isTrue();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Permit));
    }

    @Test
    void saksbehandler_skal_ha_tilgang_til_å_lese_fagsak_med_bruker_uten_beskyttelsesbehov() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of());
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "read");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isTrue();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Permit));
    }

    @Test
    void veileder_kan_ikke_lese_sak_med_kode6_person_uten_spesifikk_tilgang() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of(AdressebeskyttelseGradering.STRENGT_FORTROLIG));

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "read");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isFalse();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Deny));
    }

    @Test
    void veileder_kan_ikke_lese_sak_med_kode7_person_uten_spesifikk_tilgang() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of(AdressebeskyttelseGradering.STRENGT_FORTROLIG));

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "read");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isFalse();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Deny));
    }

    @Test
    void veileder_kan_ikke_lese_sak_med_skjermet_person_uten_spesifikk_tilgang() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), true, Set.of());

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "read");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isFalse();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Deny));
    }

    @Test
    void saksbehandler_kan_ikke_lese_sak_med_kode6_person_uten_spesifikk_tilgang() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of(AdressebeskyttelseGradering.STRENGT_FORTROLIG));

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "read");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isFalse();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Deny));
    }

    @Test
    void saksbehandler_kan_ikke_lese_sak_med_kode7_person_uten_spesifikk_tilgang() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of(AdressebeskyttelseGradering.FORTROLIG));

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "read");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isFalse();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Deny));
    }

    @Test
    void veileder_med_spesifikk_tilgang_tiL_kode6_kan_lese_sak_med_kode6_person() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of(AdressebeskyttelseGradering.STRENGT_FORTROLIG));

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgangMedKode6());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "read");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isTrue();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Permit));
    }

    @Test
    void saksbehandler_skal_ha_tilgang_til_å_opprette_fagsak() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of());

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "create");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isTrue();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Permit));
    }

    @Test
    void saksbehandler_skal_ha_tilgang_til_å_behandle_ordinært_aksjonspunkt() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of());

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "update");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE, AksjonspunktType.MANUELL.getOffisiellKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isTrue();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Permit));
    }

    @Test
    void veileder_kan_ikke_behandle_aksjonspunkt() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of());

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "update");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.UTREDES.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE, AksjonspunktType.MANUELL.getOffisiellKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isFalse();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Deny));
    }

    @Test
    void beslutter_skal_ha_tilgang_til_å_beslutte_sak() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of());

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(besluttertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "update");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.FATTE_VEDTAK.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE, AksjonspunktType.MANUELL.getOffisiellKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isTrue();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Permit));
    }

    @Test
    void beslutter_kan_ikke_beslutte_sak_hen_har_saksbehandlet_selv() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of());

        TilgangerBruker besluttertilgang = besluttertilgang();
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(besluttertilgang);

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "update");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.FATTE_VEDTAK.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE, AksjonspunktType.MANUELL.getOffisiellKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_ANSVARLIG_SAKSBEHANDLER, besluttertilgang.getBrukernavn());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isFalse();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Deny));
    }

    @Test
    void saksbehandler_kan_ikke_beslutte_sak() {
        mockPerson(PersonIdent.fra(FNR), new AktørId(AKTØR_ID), false, Set.of());

        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of(AKTØR_ID));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "update");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        request.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, AbacBehandlingStatus.FATTE_VEDTAK.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());
        request.put(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE, AksjonspunktType.MANUELL.getOffisiellKode());

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isFalse();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Deny));
    }

    private static TilgangerBruker veiledertilgang() {
        return TilgangerBruker.builder()
            .medBrukernavn("Z000000")
            .medKanVeilede(true)
            .build();
    }

    private static TilgangerBruker veiledertilgangMedKode6() {
        return TilgangerBruker.builder()
            .medBrukernavn("Z000000")
            .medKanVeilede(true)
            .medKanBehandleKode6(true)
            .build();
    }

    private static TilgangerBruker saksbehandlertilgang() {
        return TilgangerBruker.builder()
            .medBrukernavn("Z000000")
            .medKanSaksbehandle(true)
            .build();
    }

    private static TilgangerBruker besluttertilgang() {
        return TilgangerBruker.builder()
            .medBrukernavn("Z000000")
            .medKanSaksbehandle(true)
            .medKanBeslutte(true)
            .build();
    }
}
