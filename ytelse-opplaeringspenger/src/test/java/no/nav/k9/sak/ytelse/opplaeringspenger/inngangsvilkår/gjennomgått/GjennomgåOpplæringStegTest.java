package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPerioderHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

@CdiDbAwareTest
class GjennomgåOpplæringStegTest {

    private GjennomgåOpplæringSteg gjennomgåOpplæringSteg;
    @Inject
    private EntityManager entityManager;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteBean;

    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteMock;
    private Behandling behandling;
    private Periode søknadsperiode;
    private final TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);

    @BeforeEach
    void setup(){
        perioderTilVurderingTjenesteMock = spy(perioderTilVurderingTjenesteBean);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        GjennomgåttOpplæringTjeneste gjennomgåttOpplæringTjeneste = new GjennomgåttOpplæringTjeneste(vilkårResultatRepository, perioderTilVurderingTjenesteMock, uttakPerioderGrunnlagRepository, vurdertOpplæringRepository);
        gjennomgåOpplæringSteg = new GjennomgåOpplæringSteg(repositoryProvider, gjennomgåttOpplæringTjeneste);

        søknadsperiode = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now());

        scenario.leggTilVilkår(VilkårType.NØDVENDIG_OPPLÆRING, Utfall.IKKE_VURDERT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_VURDERT, søknadsperiode);
    }

    private BehandlingskontrollKontekst setupBehandlingskontekst() {
        return new BehandlingskontrollKontekst(behandling.getFagsak().getId(), behandling.getFagsak().getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
    }

    private void setupPerioderTilVurdering(BehandlingskontrollKontekst kontekst) {
        when(perioderTilVurderingTjenesteMock.utled(kontekst.getBehandlingId(), VilkårType.GJENNOMGÅ_OPPLÆRING))
            .thenReturn(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFom(), søknadsperiode.getTom()))));
    }

    private void setupUttakPerioder(Periode kursperiode, Periode reisetidTil, Periode reisetidHjem) {
        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(new JournalpostId("112233"),
            List.of(new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFom(), søknadsperiode.getTom()), Duration.ofHours(7).plusMinutes(30))),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new KursPeriode(kursperiode.getFom(), kursperiode.getTom(),
                reisetidTil != null ? DatoIntervallEntitet.fraOgMedTilOgMed(reisetidTil.getFom(), reisetidTil.getTom()) : null,
                reisetidHjem != null ? DatoIntervallEntitet.fraOgMedTilOgMed(reisetidHjem.getFom(), reisetidHjem.getTom()) : null,
                "institusjon", null, "beskrivelse")));
        uttakPerioderGrunnlagRepository.lagre(behandling.getId(), perioderFraSøknad);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), uttakPerioderGrunnlagRepository.hentGrunnlag(behandling.getId()).map(UttaksPerioderGrunnlag::getOppgitteSøknadsperioder).orElseThrow());
    }

    private void lagreGrunnlag(VurdertOpplæringGrunnlag grunnlag) {
        entityManager.persist(grunnlag.getVurdertePerioder());
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    @Test
    void skalReturnereAksjonspunktNårVurderingMangler() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(søknadsperiode, null, null);

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        BehandleStegResultat resultat = gjennomgåOpplæringSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_GJENNOMGÅTT_OPPLÆRING);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.IKKE_VURDERT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom(),
            null);
    }

    @Test
    void skalReturnereAksjonspunktNårVurderingIkkeErKomplett() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(søknadsperiode, null, null);

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(), null, null,
            new VurdertOpplæringPerioderHolder(List.of(
                new VurdertOpplæringPeriode(søknadsperiode.getFom(), søknadsperiode.getTom().minusDays(1), true, null, ""))));
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = gjennomgåOpplæringSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_GJENNOMGÅTT_OPPLÆRING);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING).orElse(null);
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
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(søknadsperiode, null, null);

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(), null, null,
            new VurdertOpplæringPerioderHolder(List.of(
                new VurdertOpplæringPeriode(søknadsperiode.getFom(), søknadsperiode.getTom().minusDays(1), true, null, ""),
                new VurdertOpplæringPeriode(søknadsperiode.getTom(), søknadsperiode.getTom(), false, null, ""))));
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = gjennomgåOpplæringSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(0);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.OPPFYLT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom().minusDays(1),
            null);
        assertVilkårPeriode(vilkår.getPerioder().get(1),
            Utfall.IKKE_OPPFYLT,
            søknadsperiode.getTom(),
            søknadsperiode.getTom(),
            Avslagsårsak.IKKE_GJENNOMGÅTT_OPPLÆRING);
    }

    @Test
    void skalReturnereUtenAksjonspunktNårTidligereVilkårIkkeErGodkjent() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(søknadsperiode, null, null);

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        BehandleStegResultat resultat = gjennomgåOpplæringSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(0);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(0);
    }

    @Test
    void skalReturnereUtenAksjonspunktNårTidligereVilkårErDelvisGodkjent() {
        Periode godkjentPeriode = new Periode(søknadsperiode.getFom().plusDays(1), søknadsperiode.getTom());
        Periode ikkeGodkjentPeriode = new Periode(søknadsperiode.getFom(), søknadsperiode.getFom());

        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_OPPFYLT, ikkeGodkjentPeriode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, godkjentPeriode);

        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(søknadsperiode, null, null);

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(), null, null,
            new VurdertOpplæringPerioderHolder(List.of(
                new VurdertOpplæringPeriode(godkjentPeriode.getFom(), godkjentPeriode.getTom(), true, null, ""))));
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = gjennomgåOpplæringSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(0);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.OPPFYLT,
            godkjentPeriode.getFom(),
            godkjentPeriode.getTom(),
            null);
    }

    @Test
    void reisetidPåEnDagSkalGodkjennesAutomatisk() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        Periode kursperiode = new Periode(søknadsperiode.getFom().plusDays(1), søknadsperiode.getTom().minusDays(1));
        setupUttakPerioder(søknadsperiode, new Periode(søknadsperiode.getFom(), søknadsperiode.getFom()), new Periode(søknadsperiode.getTom(), søknadsperiode.getTom()));

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(), null, null,
            new VurdertOpplæringPerioderHolder(List.of(
                new VurdertOpplæringPeriode(kursperiode.getFom(), kursperiode.getTom(), true, null, ""))));
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = gjennomgåOpplæringSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(0);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.OPPFYLT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom(),
            null);
    }

    @Test
    void reisetidPåOverEnDagSkalIkkeGodkjennesAutomatisk() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        Periode kursperiode = new Periode(søknadsperiode.getFom().plusDays(2), søknadsperiode.getTom().minusDays(2));
        Periode reisetidTil = new Periode(søknadsperiode.getFom(), søknadsperiode.getFom().plusDays(1));
        Periode reisetidHjem = new Periode(søknadsperiode.getTom().minusDays(1), søknadsperiode.getTom());
        setupUttakPerioder(søknadsperiode, reisetidTil, reisetidHjem);

        var kontekst = setupBehandlingskontekst();
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(), null, null,
            new VurdertOpplæringPerioderHolder(List.of(
                new VurdertOpplæringPeriode(kursperiode.getFom(), kursperiode.getTom(), true, new VurdertReisetid(DatoIntervallEntitet.fraOgMedTilOgMed(reisetidTil.getFom(), reisetidTil.getTom()), null, ""), ""))));
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = gjennomgåOpplæringSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(0);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.OPPFYLT,
            søknadsperiode.getFom(),
            kursperiode.getTom(),
            null);
        assertVilkårPeriode(vilkår.getPerioder().get(1),
            Utfall.IKKE_OPPFYLT,
            reisetidHjem.getFom(),
            reisetidHjem.getTom(),
            Avslagsårsak.IKKE_PÅ_REISE);
    }

    private void assertVilkårPeriode(VilkårPeriode vilkårPeriode, Utfall utfall, LocalDate fom, LocalDate tom, Avslagsårsak avslagsårsak) {
        assertThat(vilkårPeriode.getUtfall()).isEqualTo(utfall);
        assertThat(vilkårPeriode.getFom()).isEqualTo(fom);
        assertThat(vilkårPeriode.getTom()).isEqualTo(tom);
        assertThat(vilkårPeriode.getAvslagsårsak()).isEqualTo(avslagsårsak);
    }
}
