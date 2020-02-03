package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.finn.unleash.FakeUnleash;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingRestKlient;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.FatterVedtakAksjonspunkt;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AksjonspunktGodkjenningDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.FatterVedtakAksjonspunktOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslaVedtakAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslåVedtakAksjonspunktOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.OpprettToTrinnsgrunnlag;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AksjonspunktOppdatererTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private TotrinnRepository totrinnRepository;

    @Inject
    private FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt;

    @Inject
    private VedtakTjeneste vedtakTjeneste;
    
    @Inject
    private OpprettToTrinnsgrunnlag opprettTotrinnsgrunnlag;

    @Test
    public void bekreft_foreslå_vedtak_aksjonspkt_setter_ansvarlig_saksbehandler() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad();

        Behandling behandling = scenario.lagre(repositoryProvider);

        ForeslaVedtakAksjonspunktDto dto = new ForeslaVedtakAksjonspunktDto("begrunnelse", null, null, false) {
        };
        ForeslåVedtakAksjonspunktOppdaterer foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
            repositoryProvider, mock(HistorikkTjenesteAdapter.class),
            opprettTotrinnsgrunnlag,
            vedtakTjeneste,
            mock(FormidlingRestKlient.class),
            new FakeUnleash()) {
            @Override
            protected String getCurrentUserId() {
                // return test verdi
                return "hello";
            }
        };
        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        assertThat(behandling.getAnsvarligSaksbehandler()).isEqualTo("hello");
    }

    @Test
    public void oppdaterer_aksjonspunkt_med_beslutters_vurdering_ved_totrinnskontroll() {

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD, BehandlingStegType.KONTROLLER_FAKTA);
        Behandling behandling = scenario.lagre(repositoryProvider);

        var aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setArsaker(Set.of(VurderÅrsak.FEIL_FAKTA));
        aksGodkjDto.setGodkjent(false);
        String besluttersBegrunnelse = "Må ha bedre dokumentasjon.";
        aksGodkjDto.setBegrunnelse(besluttersBegrunnelse);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        var aksjonspunktDto = new FatterVedtakAksjonspunktDto("", Collections.singletonList(aksGodkjDto));
        new FatterVedtakAksjonspunktOppdaterer(fatterVedtakAksjonspunkt).oppdater(aksjonspunktDto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), aksjonspunktDto));

        Collection<Totrinnsvurdering> totrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);
        assertThat(totrinnsvurderinger).hasSize(1);
        Totrinnsvurdering totrinnsvurdering = totrinnsvurderinger.iterator().next();

        assertThat(totrinnsvurdering.isGodkjent()).isFalse();
        assertThat(totrinnsvurdering.getBegrunnelse()).isEqualTo(besluttersBegrunnelse);
        assertThat(totrinnsvurdering.getVurderPåNyttÅrsaker()).hasSize(1);
        VurderÅrsakTotrinnsvurdering vurderPåNyttÅrsak = totrinnsvurdering.getVurderPåNyttÅrsaker().iterator().next();
        assertThat(vurderPåNyttÅrsak.getÅrsaksType()).isEqualTo(VurderÅrsak.FEIL_FAKTA);
    }

    @Test
    public void oppdaterer_aksjonspunkt_med_godkjent_totrinnskontroll() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD, BehandlingStegType.KONTROLLER_FAKTA);

        Behandling behandling = scenario.lagre(repositoryProvider);

        var aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setGodkjent(true);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        var aksjonspunktDto = new FatterVedtakAksjonspunktDto("", Collections.singletonList(aksGodkjDto));
        new FatterVedtakAksjonspunktOppdaterer(fatterVedtakAksjonspunkt).oppdater(aksjonspunktDto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), aksjonspunktDto));

        Collection<Totrinnsvurdering> totrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);
        assertThat(totrinnsvurderinger).hasSize(1);
        Totrinnsvurdering totrinnsvurdering = totrinnsvurderinger.iterator().next();

        assertThat(totrinnsvurdering.isGodkjent()).isTrue();
        assertThat(totrinnsvurdering.getBegrunnelse()).isNullOrEmpty();
        assertThat(totrinnsvurdering.getVurderPåNyttÅrsaker()).isEmpty();
    }

}
