package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.skjæringstidspunkt.DefaultSkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.kontrakt.behandling.BehandlingÅrsakDto;
import no.nav.k9.sak.kontrakt.behandling.UtvidetBehandlingDto;

public class BehandlingÅrsakDtoTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private Behandling behandling;
    private BehandlingDtoTjeneste behandlingDtoTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository = new TilbakekrevingRepository(repoRule.getEntityManager());
    private FordelingRepository fordelingRepository = new FordelingRepository(repoRule.getEntityManager());
    private FakeUnleash unleash = new FakeUnleash();

    @Before
    public void setup() {
        skjæringstidspunktTjeneste = new DefaultSkjæringstidspunktTjenesteImpl(repositoryProvider.getBehandlingRepository(), repositoryProvider.getOpptjeningRepository(), fordelingRepository);
        var beregningsgrunnlagTjeneste = new HentBeregningsgrunnlagTjeneste(repoRule.getEntityManager());
        behandlingDtoTjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningsgrunnlagTjeneste, tilbakekrevingRepository, skjæringstidspunktTjeneste, null, unleash);

        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);
        var behandlingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING)
            .medManueltOpprettet(true);
        behandlingÅrsak.buildFor(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

    }

    @Test
    public void skal_teste_at_behandlingÅrsakDto_får_korrekte_verdier() {

        UtvidetBehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, null);

        List<BehandlingÅrsakDto> årsaker = dto.getBehandlingÅrsaker();

        assertThat(årsaker).isNotNull();
        assertThat(årsaker).hasSize(1);
        assertThat(årsaker.get(0).getBehandlingArsakType()).isEqualTo(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING);
        assertThat(årsaker.get(0).isManueltOpprettet()).isTrue();

    }
}
