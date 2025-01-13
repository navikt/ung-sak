package no.nav.ung.sak.tilgangskontroll;

import no.nav.k9.felles.sikkerhet.abac.Decision;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.ung.abac.BeskyttetRessursKoder;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.ung.sak.tilgangskontroll.api.AbacAttributter;
import no.nav.ung.sak.tilgangskontroll.api.AbacBehandlingStatus;
import no.nav.ung.sak.tilgangskontroll.api.AbacFagsakStatus;
import no.nav.ung.sak.tilgangskontroll.tilganger.*;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDecisionPointTest {

    private AnsattTilgangerTjeneste ansattTilgangerTjeneste = Mockito.mock(AnsattTilgangerTjeneste.class);
    private PersonDiskresjonskodeTjeneste personDiskresjonskodeTjeneste = Mockito.mock(PersonDiskresjonskodeTjeneste.class);
    private TilgangTilPersonTjeneste tilgangTilPersonTjeneste = new TilgangTilPersonTjeneste(personDiskresjonskodeTjeneste);
    private TilgangTilOperasjonTjeneste tilgangTilOperasjonTjeneste = new TilgangTilOperasjonTjeneste();

    private PolicyDecisionPoint pdp = new PolicyDecisionPoint(ansattTilgangerTjeneste, tilgangTilPersonTjeneste, tilgangTilOperasjonTjeneste);

    @Test
    void veileder_skal_ha_tilgang_til_å_lese_fagsak_med_bruker_uten_beskyttelsesbehov() {
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of());
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of());
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of(Diskresjonskode.KODE6));
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of(Diskresjonskode.KODE7));
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of(Diskresjonskode.SKJERMET));
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of(Diskresjonskode.KODE6));
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of(Diskresjonskode.KODE7));
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of(Diskresjonskode.KODE6));
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgangMedKode6());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of());
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, "create");
        request.put(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);

        Tilgangsbeslutning tilgangsbeslutning = pdp.vurderTilgangForInnloggetBruker(request);
        assertThat(tilgangsbeslutning.fikkTilgang()).isTrue();
        assertThat(tilgangsbeslutning.getDelbeslutninger()).isEqualTo(List.of(Decision.Permit));
    }

    @Test
    void saksbehandler_skal_ha_tilgang_til_å_behandle_ordinært_aksjonspunkt() {
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of());
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of());
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(veiledertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of());
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(besluttertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of());
        TilgangerBruker besluttertilgang = besluttertilgang();
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(besluttertilgang);

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
        Mockito.when(personDiskresjonskodeTjeneste.hentDiskresjonskoder(Mockito.any(AktørId.class))).thenReturn(Set.of());
        Mockito.when(ansattTilgangerTjeneste.tilgangerForInnloggetBruker()).thenReturn(saksbehandlertilgang());

        PdpRequest request = new PdpRequest();
        request.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, List.of("1234567890123"));
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
