package no.nav.k9.sak.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingÅrsakDto;
import no.nav.k9.sak.skjæringstidspunkt.DefaultSkjæringstidspunktTjenesteImpl;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BehandlingÅrsakDtoTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private SøknadRepository søknadRepository;

    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    private TilbakekrevingRepository tilbakekrevingRepository;

    @Inject
    private UttakRepository uttakRepository;

    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    private Behandling behandling;
    private BehandlingDtoTjeneste behandlingDtoTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Before
    public void setup() {
        skjæringstidspunktTjeneste = new DefaultSkjæringstidspunktTjenesteImpl(behandlingRepository, repositoryProvider.getOpptjeningRepository(), uttakRepository, vilkårResultatRepository);
        behandlingDtoTjeneste = new BehandlingDtoTjeneste(fagsakRepository, behandlingRepository, behandlingVedtakRepository, søknadRepository, uttakRepository, tilbakekrevingRepository, skjæringstidspunktTjeneste, vilkårResultatRepository, "/k9/oppdrag/api");

        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);
        var behandlingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING)
            .medManueltOpprettet(true);
        behandlingÅrsak.buildFor(behandling);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

    }

    @Test
    public void skal_teste_at_behandlingÅrsakDto_får_korrekte_verdier() {

        BehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, null);

        List<BehandlingÅrsakDto> årsaker = dto.getBehandlingÅrsaker();

        assertThat(årsaker).isNotNull();
        assertThat(årsaker).hasSize(1);
        assertThat(årsaker.get(0).getBehandlingArsakType()).isEqualTo(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING);
        assertThat(årsaker.get(0).isManueltOpprettet()).isTrue();

    }
}
