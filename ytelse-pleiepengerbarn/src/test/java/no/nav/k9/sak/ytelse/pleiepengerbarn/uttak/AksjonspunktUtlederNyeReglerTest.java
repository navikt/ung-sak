package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet.AktivitetstatusOgArbeidsgiver;
import no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet.TilkommetAktivitetTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Barn;
import no.nav.pleiepengerbarn.uttak.kontrakter.Søker;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.YtelseType;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
public class AksjonspunktUtlederNyeReglerTest {

    @Inject
    private AksjonspunktUtlederNyeRegler utleder;
    private TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste = mock(TilkommetAktivitetTjeneste.class);
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;

    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste = mock(MapInputTilUttakTjeneste.class);
    private Behandling førstegangsbehandling;
    private Behandling revurdering;
    private UttakNyeReglerRepository uttakNyeReglerRepository = mock(UttakNyeReglerRepository.class);
    @Inject
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste = mock(SøknadsperiodeTjeneste.class);

    @BeforeEach
    void setUp() {
        utleder = new AksjonspunktUtlederNyeRegler(behandlingRepository, uttakNyeReglerRepository, tilkommetAktivitetTjeneste, aksjonspunktKontrollRepository, søknadsperiodeTjeneste, mapInputTilUttakTjeneste, true);

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.DAGPENGER, new AktørId(123L), new Saksnummer("987"), LocalDate.now(), LocalDate.now());
        fagsakRepository.opprettNy(fagsak);
        førstegangsbehandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(førstegangsbehandling, behandlingRepository.taSkriveLås(førstegangsbehandling.getId()));

        revurdering = Behandling.fraTidligereBehandling(førstegangsbehandling, BehandlingType.REVURDERING).medBehandlingStatus(BehandlingStatus.UTREDES).build();

        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering.getId()));

    }

    @Test
    void skal_få_aksjonspunkt_for_førstegangsbehandling_uten_dato_satt() {
        LocalDate dag1 = LocalDate.now();
        LocalDate dag2 = dag1.plusDays(1);
        DatoIntervallEntitet søknadsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(dag1, dag2);
        when(søknadsperiodeTjeneste.utledFullstendigPeriode(anyLong())).thenReturn(new TreeSet<>(Set.of(søknadsperiode)));
        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any(LocalDateInterval.class))).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(mapInputTilUttakTjeneste.hentUtOgMapRequestUtenInntektsgradering(BehandlingReferanse.fra(førstegangsbehandling))).thenReturn(lagUttaksgrunnlag());

        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(førstegangsbehandling);

        assertThat(aksjonspunktDefinisjon).isPresent();
        assertThat(aksjonspunktDefinisjon.get()).isEqualTo(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
    }

    @Test
    void skal_få_aksjonspunkt_for_revurdering_uten_dato_satt() {
        LocalDate dag1 = LocalDate.now();
        LocalDate dag2 = dag1.plusDays(1);
        DatoIntervallEntitet søknadsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(dag1, dag2);
        when(søknadsperiodeTjeneste.utledFullstendigPeriode(anyLong())).thenReturn(new TreeSet<>(Set.of(søknadsperiode)));
        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any(LocalDateInterval.class))).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(mapInputTilUttakTjeneste.hentUtOgMapRequestUtenInntektsgradering(BehandlingReferanse.fra(revurdering))).thenReturn(lagUttaksgrunnlag());

        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(revurdering);

        assertThat(aksjonspunktDefinisjon).isPresent();
        assertThat(aksjonspunktDefinisjon.get()).isEqualTo(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
    }

    @Test
    public void skal_få_ikke_få_aksjonspunkt_for_førstegangsbehandling_med_dato_satt_og_med_eksisterende_aksjonspunkt() {

        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any(LocalDateInterval.class))).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(mapInputTilUttakTjeneste.hentUtOgMapRequestUtenInntektsgradering(BehandlingReferanse.fra(førstegangsbehandling))).thenReturn(lagUttaksgrunnlag());
        when(uttakNyeReglerRepository.finnDatoForNyeRegler(any())).thenReturn(Optional.of(LocalDate.now()));
        aksjonspunktKontrollRepository.leggTilAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);


        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(førstegangsbehandling);

        assertThat(aksjonspunktDefinisjon).isEmpty();
    }

    @Test
    void skal_få_ikke_få_aksjonspunkt_for_revurderig_med_dato_satt_og_med_eksisterende_aksjonspunkt() {

        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any(LocalDateInterval.class))).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(mapInputTilUttakTjeneste.hentUtOgMapRequestUtenInntektsgradering(BehandlingReferanse.fra(førstegangsbehandling))).thenReturn(lagUttaksgrunnlag());
        when(uttakNyeReglerRepository.finnDatoForNyeRegler(any())).thenReturn(Optional.of(LocalDate.now()));
        lagUtførtAksjonspunkt(førstegangsbehandling, "En fin begrunnelse", "VELDIG_ANSVARLIG_SAKSBEHANDLER");
        aksjonspunktKontrollRepository.leggTilAksjonspunkt(revurdering, AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        lagUtførtAksjonspunkt(revurdering, "En enda finere begrunnelse", "VELDIG_UANSVARLIG_SAKSBEHANDLER");

        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(revurdering);

        assertThat(aksjonspunktDefinisjon).isEmpty();


        var aksjonspunkter = revurdering.getAksjonspunkter();
        assertThat(aksjonspunkter.size()).isEqualTo(1);
        var revurderingAP = aksjonspunkter.iterator().next();
        assertThat(revurderingAP.getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        assertThat(revurderingAP.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT);
        assertThat(revurderingAP.getBegrunnelse()).isEqualTo("En enda finere begrunnelse");
        assertThat(revurderingAP.getAnsvarligSaksbehandler()).isEqualTo("VELDIG_UANSVARLIG_SAKSBEHANDLER");
        assertThat(revurderingAP.isToTrinnsBehandling()).isFalse();
    }

    @Test
    void skal_få_utført_aksjonspunkt_for_revurdering_med_dato_satt_og_uten_eksisterende_aksjonspunkt() {

        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any(LocalDateInterval.class))).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(mapInputTilUttakTjeneste.hentUtOgMapRequestUtenInntektsgradering(BehandlingReferanse.fra(førstegangsbehandling))).thenReturn(lagUttaksgrunnlag());
        when(uttakNyeReglerRepository.finnDatoForNyeRegler(any())).thenReturn(Optional.of(LocalDate.now()));
        lagUtførtAksjonspunkt(førstegangsbehandling, "En fin begrunnelse", "VELDIG_ANSVARLIG_SAKSBEHANDLER");


        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(revurdering);

        assertThat(aksjonspunktDefinisjon).isEmpty();

        var aksjonspunkter = revurdering.getAksjonspunkter();
        assertThat(aksjonspunkter.size()).isEqualTo(1);
        var revurderingAP = aksjonspunkter.iterator().next();
        assertThat(revurderingAP.getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        assertThat(revurderingAP.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT);
        assertThat(revurderingAP.getBegrunnelse()).isEqualTo("En fin begrunnelse");
        assertThat(revurderingAP.getAnsvarligSaksbehandler()).isEqualTo("VELDIG_ANSVARLIG_SAKSBEHANDLER");
        assertThat(revurderingAP.isToTrinnsBehandling()).isFalse();
    }

    private void lagUtførtAksjonspunkt(Behandling behandling, String begrunnelse, String ansvarligSaksbehandler) {
        var ap = aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        aksjonspunktKontrollRepository.setTilUtført(ap, begrunnelse);
        ap.setAnsvarligSaksbehandler(ansvarligSaksbehandler);
    }

    private static Uttaksgrunnlag lagUttaksgrunnlag() {
        return new Uttaksgrunnlag(
            YtelseType.PSB,
            new Barn(AktørId.dummy().getAktørId(), null, null),
            new Søker(AktørId.dummy().getAktørId(), null),
            "TEST",
            UUID.randomUUID().toString(),
            List.of(),

            List.of(),
            List.of(),
            Map.of(),
            null,
            Map.of(),
            Map.of(),
            List.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }


}
