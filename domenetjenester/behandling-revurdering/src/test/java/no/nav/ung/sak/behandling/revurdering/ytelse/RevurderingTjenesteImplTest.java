package no.nav.ung.sak.behandling.revurdering.ytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.ung.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.ung.sak.behandling.revurdering.RevurderingTjeneste;
import no.nav.ung.sak.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.ung.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class RevurderingTjenesteImplTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private HistorikkRepository historikkRepository;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    @FagsakYtelseTypeRef
    private RevurderingEndring revurderingEndring;

    @Inject
    @Any
    private Instance<GrunnlagKopierer> grunnlagKopierer;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        opprettRevurderingsKandidat();
    }

    @Test
    public void skal_opprette_revurdering() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        scenario.medBehandlingstidFrist(LocalDate.now().minusDays(5));
        Behandling behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);

        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);

        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        var revurderingTjeneste = new RevurderingTjeneste(behandlingskontrollTjeneste,
            revurderingTjenesteFelles, grunnlagKopierer, historikkRepository);

        // Act
        Behandling revurdering = revurderingTjeneste
            .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes, BehandlingÅrsakType.RE_ANNET, new OrganisasjonsEnhet(null, null));

        // Assert
        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType()).isEqualTo(BehandlingÅrsakType.RE_ANNET);
        assertThat(revurdering.getType()).isEqualTo(BehandlingType.REVURDERING);
        assertThat(revurdering.getAksjonspunkter()).isEmpty();
        assertThat(revurdering.getBehandlingstidFrist()).isNotEqualTo(behandlingSomSkalRevurderes.getBehandlingstidFrist());
    }

    private void opprettRevurderingsKandidat() {

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.lagre(repositoryProvider);
    }
}
