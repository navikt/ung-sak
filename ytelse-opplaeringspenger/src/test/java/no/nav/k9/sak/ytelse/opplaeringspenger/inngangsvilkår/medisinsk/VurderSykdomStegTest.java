package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomAksjonspunkt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

@CdiDbAwareTest
class VurderSykdomStegTest {

    private VurderSykdomSteg vurderSykdomSteg;
    @Inject
    private EntityManager entityManager;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteBean;
    @Inject
    private SykdomVurderingTjeneste sykdomVurderingTjenesteBean;
    @Inject
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    @Inject
    private SøknadsperiodeTjeneste søknadsperiodeTjenesteBean;

    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteMock;
    private SøknadsperiodeTjeneste søknadsperiodeTjenesteMock;
    private SykdomVurderingTjeneste sykdomVurderingTjenesteMock;
    private Behandling behandling;
    private final Periode søknadsperiode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now());
    private final TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);

    @BeforeEach
    void setup(){
        perioderTilVurderingTjenesteMock = spy(perioderTilVurderingTjenesteBean);
        søknadsperiodeTjenesteMock = spy(søknadsperiodeTjenesteBean);
        sykdomVurderingTjenesteMock = spy(sykdomVurderingTjenesteBean);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderSykdomSteg = new VurderSykdomSteg(repositoryProvider, perioderTilVurderingTjenesteMock, sykdomVurderingTjenesteMock, medisinskGrunnlagRepository, søknadsperiodeTjenesteMock);

        scenario.leggTilVilkår(VilkårType.NØDVENDIG_OPPLÆRING, Utfall.IKKE_VURDERT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_VURDERT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_VURDERT, søknadsperiode);

        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId barnAktørId = AktørId.dummy();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .barn(barnAktørId, LocalDate.now().plusDays(7))
            .relasjonTil(søkerAktørId, RelasjonsRolleType.MORA, null)
            .build();
        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .personstatus(PersonstatusType.BOSA)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, null)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);
        scenario.medPleietrengende(barnAktørId);
    }

    private BehandlingskontrollKontekst setupBehandlingskontekst() {
        return new BehandlingskontrollKontekst(behandling.getFagsak().getId(), behandling.getFagsak().getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
    }

    private void setupPerioderTilVurdering(BehandlingskontrollKontekst kontekst) {
        when(perioderTilVurderingTjenesteMock.utled(kontekst.getBehandlingId(), VilkårType.LANGVARIG_SYKDOM))
            .thenReturn(new TreeSet<>(List.of(DatoIntervallEntitet.fra(søknadsperiode))));

        when(søknadsperiodeTjenesteMock.utledFullstendigPeriode(kontekst.getBehandlingId())).thenReturn(new TreeSet<>(List.of(DatoIntervallEntitet.fra(søknadsperiode))));
    }

    @Test
    void skalReturnereAksjonspunktNårVurderingMangler() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        BehandleStegResultat resultat = vurderSykdomSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.LANGVARIG_SYKDOM).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.IKKE_VURDERT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom(),
            null);
    }

    @Test
    void skalReturnereUtenAksjonspunktNårVurderingErKomplett() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        when(sykdomVurderingTjenesteMock.vurderAksjonspunkt(behandling)).thenReturn(SykdomAksjonspunkt.bareFalse());

        BehandleStegResultat resultat = vurderSykdomSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.LANGVARIG_SYKDOM).orElse(null);
        assertThat(vilkår).isNotNull();
        LocalDateTimeline<Boolean> tidslinjeUtenHelg = Hjelpetidslinjer.fjernHelger(TidslinjeUtil.tilTidslinjeKomprimert(List.of(søknadsperiode)));
        assertThat(vilkår.getPerioder()).hasSize(tidslinjeUtenHelg.getLocalDateIntervals().size());
        int i = 0;
        for (LocalDateInterval interval : tidslinjeUtenHelg.getLocalDateIntervals()) {
            assertVilkårPeriode(vilkår.getPerioder().get(i++),
                Utfall.IKKE_OPPFYLT,
                interval.getFomDato(),
                interval.getTomDato(),
                Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE);
        }
    }

    @Test
    void skalReturnereUtenAksjonspunktNårInstitusjonIkkeErGodkjent() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.IKKE_OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        BehandleStegResultat resultat = vurderSykdomSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.LANGVARIG_SYKDOM).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(0);
    }

    private void assertVilkårPeriode(VilkårPeriode vilkårPeriode, Utfall utfall, LocalDate fom, LocalDate tom, Avslagsårsak avslagsårsak) {
        assertThat(vilkårPeriode.getUtfall()).isEqualTo(utfall);
        assertThat(vilkårPeriode.getFom()).isEqualTo(fom);
        assertThat(vilkårPeriode.getTom()).isEqualTo(tom);
        assertThat(vilkårPeriode.getAvslagsårsak()).isEqualTo(avslagsårsak);
    }
}
