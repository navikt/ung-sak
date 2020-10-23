package no.nav.k9.sak.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BehandlingDtoTjenesteImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

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

    private BehandlingDtoTjeneste tjeneste;

    private Collection<ResourceLink> existingRoutes;

    @Before
    public void setUp() {
        existingRoutes = RestUtils.getRoutes();
        tjeneste = new BehandlingDtoTjeneste(fagsakRepository, behandlingRepository, behandlingVedtakRepository, søknadRepository, uttakRepository, tilbakekrevingRepository, vilkårResultatRepository, "/k9/oppdrag/api");
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
        if (dtoLink.getRel().equals("brev-maler")) {
            return true;
        }
        if (dtoLink.getRel().equals("tilgjengelige-vedtaksbrev")) {
            return true;
        }
        if (dtoLink.getRel().equals("dokumentdata-lagre")) {
            return true;
        }
        if (dtoLink.getRel().equals("dokumentdata-hente")) {
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
