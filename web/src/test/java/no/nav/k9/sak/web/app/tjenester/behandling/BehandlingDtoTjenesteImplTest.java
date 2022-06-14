package no.nav.k9.sak.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.registerinnhenting.InformasjonselementerUtleder;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingDtoTjenesteImplTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private UttakRepository uttakRepository;

    @Inject
    private SøknadRepository søknadRepository;

    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    private TilbakekrevingRepository tilbakekrevingRepository;

    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    private TotrinnTjeneste totrinnTjeneste;

    @Inject
    @Any
    private Instance<InformasjonselementerUtleder> informasjonselementer;

    private BehandlingDtoTjeneste tjeneste;

    private Collection<ResourceLink> existingRoutes;

    @BeforeEach
    public void setUp() {
        existingRoutes = RestUtils.getRoutes();
        tjeneste = new BehandlingDtoTjeneste(behandlingRepository, behandlingVedtakRepository, søknadRepository, tilbakekrevingRepository, vilkårResultatRepository,
            totrinnTjeneste, informasjonselementer, "/k9/oppdrag/api");
    }

    @Test
    public void alle_paths_skal_eksistere() {
        Set<Behandling> behandlinger = new HashSet<>();
        behandlinger.add(lagBehandling());
        for (Behandling behandling : behandlinger) {
            List<ResourceLink> links = tjeneste.lagUtvidetBehandlingDto(behandling, null).getLinks();
            for (ResourceLink dtoLink : links) {
                assertThat(routeExists(dtoLink)).withFailMessage("Route rel= " + dtoLink.getRel() + " - " + dtoLink.toString() + " does not exist.").isTrue();
            }
            List<ResourceLink> revurderingLinks = tjeneste.lagUtvidetBehandlingDtoForRevurderingensOriginalBehandling(behandling).getLinks();
            for (ResourceLink dtoLink : revurderingLinks) {
                assertThat(routeExists(dtoLink)).withFailMessage("Route rel= " + dtoLink.getRel() + " - " + dtoLink.toString() + " does not exist.").isTrue();
            }
        }
    }

    private Boolean routeExists(ResourceLink dtoLink) {
        var linkEksists = false;
        if (dtoLink.getErEksternAdresse()) {
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

}
