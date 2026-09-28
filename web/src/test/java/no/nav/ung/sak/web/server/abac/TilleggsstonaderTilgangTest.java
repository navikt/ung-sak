package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.konfigurasjon.env.Cluster;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtSamling;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.k9.felles.sikkerhet.abac.ÅrsakIkkeTilgang;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TilleggsstonaderTilgangTest {

    private static final String DUMMY_ID_TOKEN = "dummyheader.dymmypayload.dummysignaturee";
    private static final String CLUSTER = Environment.current().getCluster().clusterName();
    private static final String TILLEGGSSTØNADER_AZP = CLUSTER + ":tilleggsstonader:tilleggsstonader-integrasjoner";

    private final PipRepository pipRepository = mock(PipRepository.class);
    private final AppPdpRequestBuilderImpl requestBuilder = new AppPdpRequestBuilderImpl(pipRepository, mock(AktørTjeneste.class));

    @Test
    void skal_gi_tilleggsstonader_tilgang_til_ekstern_ressurstype_med_read() {
        var attributter = attributter(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD, BeskyttetRessursActionType.READ);

        assertThat(requestBuilder.internAzureConsumer(TILLEGGSSTØNADER_AZP, attributter)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = BeskyttetRessursResourceType.class, names = "EKSTERN_SYSTEM_TILLEGGSSTØNAD", mode = EnumSource.Mode.EXCLUDE)
    void skal_nekte_tilleggsstonader_tilgang_til_andre_ressurstyper(BeskyttetRessursResourceType ressurstype) {
        var attributter = attributter(ressurstype, BeskyttetRessursActionType.READ);

        assertThat(requestBuilder.internAzureConsumer(TILLEGGSSTØNADER_AZP, attributter)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = BeskyttetRessursActionType.class, names = "READ", mode = EnumSource.Mode.EXCLUDE)
    void skal_nekte_tilleggsstonader_andre_actions_enn_read(BeskyttetRessursActionType action) {
        var attributter = attributter(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD, action);

        assertThat(requestBuilder.internAzureConsumer(TILLEGGSSTØNADER_AZP, attributter)).isFalse();
    }

    @Test
    void skal_nekte_tilleggsstonader_uten_action() {
        var attributter = attributter(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD, null);

        assertThat(requestBuilder.internAzureConsumer(TILLEGGSSTØNADER_AZP, attributter)).isFalse();
    }

    @Test
    void skal_nekte_azp_som_bare_ligner() {
        var attributter = attributter(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD, BeskyttetRessursActionType.READ);

        assertThat(requestBuilder.internAzureConsumer(TILLEGGSSTØNADER_AZP + "-x", attributter)).isFalse();
        assertThat(requestBuilder.internAzureConsumer(TILLEGGSSTØNADER_AZP.toUpperCase(), attributter)).isFalse();
        assertThat(requestBuilder.internAzureConsumer(annetCluster() + ":tilleggsstonader:tilleggsstonader-integrasjoner", attributter)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"tilleggsstonader-sak", "tilleggsstonader-oppfolging"})
    void skal_nekte_andre_apper_i_tilleggsstonader_namespace(String app) {
        var attributter = attributter(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD, BeskyttetRessursActionType.READ);

        assertThat(requestBuilder.internAzureConsumer(CLUSTER + ":tilleggsstonader:" + app, attributter)).isFalse();
    }

    @Test
    void interne_namespaces_skal_fungere_som_før() {
        var fagsak = attributter(BeskyttetRessursResourceType.FAGSAK, BeskyttetRessursActionType.UPDATE);
        var ekstern = attributter(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD, BeskyttetRessursActionType.READ);

        for (String azp : new String[]{
            CLUSTER + ":k9saksbehandling:k9-sak",
            Cluster.DEV_GCP.clusterName() + ":dusseldorf:ung-deltakelse-opplyser",
            Cluster.PROD_GCP.clusterName() + ":dusseldorf:ung-deltakelse-opplyser"}) {
            assertThat(requestBuilder.internAzureConsumer(azp, fagsak)).isEqualTo(requestBuilder.internAzureConsumer(azp)).isTrue();
            assertThat(requestBuilder.internAzureConsumer(azp, ekstern)).isTrue();
        }
    }

    @Test
    void ukjent_namespace_skal_nektes_som_før() {
        var attributter = attributter(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD, BeskyttetRessursActionType.READ);

        assertThat(requestBuilder.internAzureConsumer(CLUSTER + ":annet-namespace:app", attributter)).isFalse();
    }

    @Test
    void skal_lage_pdp_request_uten_oppslag_for_ekstern_ressurstype() {
        var attributter = attributter(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD, BeskyttetRessursActionType.READ);

        PdpRequest pdpRequest = requestBuilder.lagPdpRequest(attributter);

        assertThat(pdpRequest.getResourceType()).isEqualTo(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD);
        assertThat(pdpRequest.getAktørIder()).isEmpty();
        assertThat(pdpRequest.getFødselsnumre()).isEmpty();
        verifyNoInteractions(pipRepository);
    }

    @Test
    void saksbehandlertoken_mot_ekstern_ressurstype_skal_gi_avslag_og_ikke_500() {
        var sifAbacPdpRestKlient = mock(SifAbacPdpRestKlient.class);
        var pdpKlient = new AppPdpKlient(sifAbacPdpRestKlient);
        var pdpRequest = requestBuilder.lagPdpRequest(attributter(BeskyttetRessursResourceType.EKSTERN_SYSTEM_TILLEGGSSTØNAD, BeskyttetRessursActionType.READ));

        Tilgangsbeslutning beslutning = pdpKlient.forespørTilgang(pdpRequest);

        assertThat(beslutning.fikkTilgang()).isFalse();
        assertThat(beslutning.getÅrsakIkkeTilgang()).containsExactly(ÅrsakIkkeTilgang.HAR_IKKE_TILGANG_TIL_APPLIKASJONEN);
        verifyNoInteractions(sifAbacPdpRestKlient);
    }

    private static AbacAttributtSamling attributter(BeskyttetRessursResourceType ressurstype, BeskyttetRessursActionType action) {
        return AbacAttributtSamling.medJwtToken(DUMMY_ID_TOKEN)
            .setResourceType(ressurstype)
            .setActionType(action);
    }

    private static String annetCluster() {
        return Cluster.DEV_GCP.clusterName().equals(CLUSTER) ? Cluster.PROD_GCP.clusterName() : Cluster.DEV_GCP.clusterName();
    }
}
