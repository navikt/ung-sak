package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UtvidetBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.util.RestUtils;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BehandlingDtoTjenesteImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    
    @Inject
    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    private TilbakekrevingRepository tilbakekrevingRepository;

    private FakeUnleash unleash = new FakeUnleash();

    private BehandlingDtoTjeneste tjeneste;

    private Collection<ResourceLink> existingRoutes;

    @Before
    public void setUp() {
        existingRoutes = RestUtils.getRoutes();
        tjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningsgrunnlagTjeneste, tilbakekrevingRepository, skjæringstidspunktTjeneste, null, unleash);
    }

    @Test
    public void skal_ha_med_simuleringsresultatURL() {
        Behandling behandling = lagBehandling();

        UtvidetBehandlingDto dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);

        assertThat(getLinkRel(dto)).contains("simuleringResultat");
        assertThat(getLinkHref(dto)).contains(URI.create("/oppdrag/api/simulering/resultat-uten-inntrekk"));
    }

    @Test
    public void skal_ha_med_tilbakekrevings_link_når_det_finnes_et_resultat() {
        Behandling behandling = lagBehandling();

        tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, "varsel"));

        UtvidetBehandlingDto dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var href = RestUtils.getApiPath(TilbakekrevingRestTjeneste.VALG_PATH);
        var link = ResourceLink.get(href, "", new BehandlingUuidDto(dto.getUuid()));
        assertThat(getLinkRel(dto)).contains("tilbakekrevingvalg");
        assertThat(getLinkHref(dto)).contains(link.getHref());
    }

    @Test
    public void skal_ikke_ha_med_tilbakekrevings_link_når_det_ikke_finnes_et_resultat() {
        Behandling behandling = lagBehandling();

        UtvidetBehandlingDto dto = tjeneste.lagUtvidetBehandlingDto(behandling, null);
        var href = RestUtils.getApiPath(TilbakekrevingRestTjeneste.VALG_PATH);
        var link = ResourceLink.get(href, "", new BehandlingUuidDto(dto.getUuid()));
        assertThat(getLinkRel(dto)).doesNotContain("tilbakekrevingvalg");
        assertThat(getLinkHref(dto)).doesNotContain(link.getHref());
    }

    @Test
    public void alle_paths_skal_eksistere() {
        Set<Behandling> behandlinger = new HashSet<>();
        behandlinger.add(lagBehandling());
        for (Behandling behandling : behandlinger) {
            for (ResourceLink dtoLink : tjeneste.lagUtvidetBehandlingDto(behandling, null).getLinks()) {
                assertThat(routeExists(dtoLink)).withFailMessage("Route " + dtoLink.toString() + " does not exist.").isTrue();
            }
            for (ResourceLink dtoLink : tjeneste.lagUtvidetBehandlingDtoForRevurderingensOriginalBehandling(behandling).getLinks()) {
                assertThat(routeExists(dtoLink)).withFailMessage("Route " + dtoLink.toString() + " does not exist.").isTrue();
            }
        }
    }

    private Boolean routeExists(ResourceLink dtoLink) {
        Boolean linkEksists = false;
        if (dtoLink.getRel().equals("simuleringResultat")) {
            return true;
        }
        for (ResourceLink routeLink : existingRoutes) {
            if (dtoLink.getHref().getPath().equals(routeLink.getHref().getPath()) && dtoLink.getType().equals(routeLink.getType())) {
                linkEksists = true;
                break;
            }
        }
        return linkEksists;
    }

    private Behandling lagBehandling() {
        return TestScenarioBuilder.builderMedSøknad()
            .lagre(repositoryProvider);
    }

    private List<URI> getLinkHref(UtvidetBehandlingDto dto) {
        return dto.getLinks().stream().map(ResourceLink::getHref).collect(Collectors.toList());
    }

    private List<String> getLinkRel(UtvidetBehandlingDto dto) {
        return dto.getLinks().stream().map(ResourceLink::getRel).collect(Collectors.toList());
    }
}
