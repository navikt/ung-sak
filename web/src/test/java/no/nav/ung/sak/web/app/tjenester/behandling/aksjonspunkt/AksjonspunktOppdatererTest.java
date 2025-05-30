package no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.vedtak.VedtakTjeneste;
import no.nav.ung.sak.domene.vedtak.impl.FatterVedtakAksjonspunkt;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.kontrakt.vedtak.AksjonspunktGodkjenningDto;
import no.nav.ung.sak.kontrakt.vedtak.FatterVedtakAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnRepository;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.ung.sak.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.FatterVedtakAksjonspunktOppdaterer;
import no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslåVedtakAksjonspunktOppdaterer;
import no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslåVedtakOppdatererTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.OpprettToTrinnsgrunnlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class AksjonspunktOppdatererTest {

    @Inject
    public EntityManager entityManager;


    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private TotrinnRepository totrinnRepository;

    @Inject
    private FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt;

    @Inject
    private VedtakTjeneste vedtakTjeneste;

    @Inject
    private OpprettToTrinnsgrunnlag opprettTotrinnsgrunnlag;


    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    public void bekreft_foreslå_vedtak_aksjonspkt_setter_ansvarlig_saksbehandler() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad();

        Behandling behandling = scenario.lagre(repositoryProvider);

        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker("hello");

        var dto = new ForeslaVedtakAksjonspunktDto("begrunnelse", null, null, false, null, false);
        var vedtaksbrevHåndterer = new ForeslåVedtakOppdatererTjeneste(
            mock(HistorikkinnslagRepository.class),
            opprettTotrinnsgrunnlag,
            vedtakTjeneste);

        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(vedtaksbrevHåndterer);

        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        assertThat(behandling.getAnsvarligSaksbehandler()).isEqualTo("hello");
    }

    @Test
    public void oppdaterer_aksjonspunkt_med_beslutters_vurdering_ved_totrinnskontroll() {

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        Behandling behandling = scenario.lagre(repositoryProvider);

        var aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setArsaker(Set.of(VurderÅrsak.FEIL_FAKTA));
        aksGodkjDto.setGodkjent(false);
        String besluttersBegrunnelse = "Må ha bedre dokumentasjon.";
        aksGodkjDto.setBegrunnelse(besluttersBegrunnelse);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

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
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        Behandling behandling = scenario.lagre(repositoryProvider);

        var aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setGodkjent(true);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

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
