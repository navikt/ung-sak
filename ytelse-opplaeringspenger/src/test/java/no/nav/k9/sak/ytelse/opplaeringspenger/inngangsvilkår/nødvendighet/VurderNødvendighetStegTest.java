package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
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
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

@CdiDbAwareTest
public class VurderNødvendighetStegTest {

    private final JournalpostId journalpostId1 = new JournalpostId("123");
    private final JournalpostId journalpostId2 = new JournalpostId("321");
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
    private PleiebehovResultatRepository resultatRepository;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteBean;
    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteMock;
    private Behandling behandling;
    private VurderNødvendighetSteg vurderNødvendighetSteg;
    private Periode søknadsperiode;
    private TestScenarioBuilder scenario;
    private final LocalDateTime nå = LocalDateTime.now();

    @BeforeEach
    public void setup() {
        perioderTilVurderingTjenesteMock = spy(perioderTilVurderingTjenesteBean);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderNødvendighetSteg = new VurderNødvendighetSteg(repositoryProvider, perioderTilVurderingTjenesteMock, resultatRepository,
            new VurderNødvendighetTjeneste(repositoryProvider, perioderTilVurderingTjenesteMock, vurdertOpplæringRepository, uttakPerioderGrunnlagRepository));
        søknadsperiode = new Periode(nå.toLocalDate().minusWeeks(2), nå.toLocalDate());
        scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);
        scenario.leggTilVilkår(VilkårType.NØDVENDIG_OPPLÆRING, Utfall.IKKE_VURDERT, søknadsperiode);
    }

    private void setupPerioderTilVurdering(BehandlingskontrollKontekst kontekst) {
        when(perioderTilVurderingTjenesteMock.utled(kontekst.getBehandlingId(), VilkårType.NØDVENDIG_OPPLÆRING))
            .thenReturn(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFom(), søknadsperiode.getTom()))));
    }

    private void setupUttakPerioder(JournalpostId journalpostId, Periode periode) {
        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()), Duration.ofHours(7).plusMinutes(30))),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new KursPeriode(periode.getFom(), periode.getTom(), null, null, null, null, null)));
        uttakPerioderGrunnlagRepository.lagre(behandling.getId(), perioderFraSøknad);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), uttakPerioderGrunnlagRepository.hentGrunnlag(behandling.getId()).map(UttaksPerioderGrunnlag::getOppgitteSøknadsperioder).orElseThrow());
    }

    private void lagreGrunnlag(VurdertOpplæringGrunnlag grunnlag) {
        entityManager.persist(grunnlag.getVurdertOpplæringHolder());
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    @Test
    public void skalReturnereAksjonspunktNårNødvendighetIkkeErVurdert() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(journalpostId1, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_NØDVENDIGHET);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.IKKE_VURDERT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom(),
            null);
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårNødvendighetErGodkjent() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(journalpostId1, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpostId1, true, "", "", nå, List.of());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            null,
            new VurdertOpplæringHolder(List.of(vurdertOpplæring)),
            null,
            null
        );
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.OPPFYLT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom(),
            null);

        var pleiebehovResultat = resultatRepository.hentHvisEksisterer(behandling.getId());
        assertThat(pleiebehovResultat).isPresent();
        assertThat(pleiebehovResultat.get().getPleieperioder().getPerioder()).hasSize(1);
        assertThat(pleiebehovResultat.get().getPleieperioder().getPerioder().get(0).getPeriode().getFomDato()).isEqualTo(søknadsperiode.getFom());
        assertThat(pleiebehovResultat.get().getPleieperioder().getPerioder().get(0).getPeriode().getTomDato()).isEqualTo(søknadsperiode.getTom());
        assertThat(pleiebehovResultat.get().getPleieperioder().getPerioder().get(0).getGrad()).isEqualTo(Pleiegrad.NØDVENDIG_OPPLÆRING);
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårNødvendighetIkkeErGodkjent() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(journalpostId1, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpostId1, false, "test", "", nå, List.of());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            null,
            new VurdertOpplæringHolder(List.of(vurdertOpplæring)),
            null,
            null
        );
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.IKKE_OPPFYLT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom(),
            Avslagsårsak.IKKE_NØDVENDIG_OPPLÆRING);

        var pleiebehovResultat = resultatRepository.hentHvisEksisterer(behandling.getId());
        assertThat(pleiebehovResultat).isPresent();
        assertThat(pleiebehovResultat.get().getPleieperioder().getPerioder()).hasSize(1);
        assertThat(pleiebehovResultat.get().getPleieperioder().getPerioder().get(0).getPeriode().getFomDato()).isEqualTo(søknadsperiode.getFom());
        assertThat(pleiebehovResultat.get().getPleieperioder().getPerioder().get(0).getPeriode().getTomDato()).isEqualTo(søknadsperiode.getTom());
        assertThat(pleiebehovResultat.get().getPleieperioder().getPerioder().get(0).getGrad()).isEqualTo(Pleiegrad.INGEN);
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårInstitusjonsvilkårIkkeErOppfylt() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.IKKE_OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(journalpostId1, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).isEmpty();
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårNødvendighetErPeriodevisGodkjentOgIkkeGodkjent() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);

        Periode søknadsperiode1 = new Periode(søknadsperiode.getFom(), søknadsperiode.getTom().minusDays(1));
        setupUttakPerioder(journalpostId1, søknadsperiode1);
        Periode søknadsperiode2 = new Periode(søknadsperiode.getTom(), søknadsperiode.getTom());
        setupUttakPerioder(journalpostId2, søknadsperiode2);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæring vurdertOpplæring1 = new VurdertOpplæring(journalpostId1, true, "test", "", nå, List.of());
        VurdertOpplæring vurdertOpplæring2 = new VurdertOpplæring(journalpostId2, false, "tast", "", nå, List.of());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            null,
            new VurdertOpplæringHolder(List.of(vurdertOpplæring1, vurdertOpplæring2)),
            null,
            null
        );
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
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
            Avslagsårsak.IKKE_NØDVENDIG_OPPLÆRING);
    }

    @Test
    public void skalReturnereAksjonspunktNårVurderingIkkeErKomplett() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);

        setupUttakPerioder(journalpostId1, new Periode(søknadsperiode.getFom(), søknadsperiode.getTom().minusDays(1)));
        setupUttakPerioder(journalpostId2, new Periode(søknadsperiode.getTom(), søknadsperiode.getTom()));

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpostId1, true, "test", "", nå, List.of());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            null,
            new VurdertOpplæringHolder(List.of(vurdertOpplæring)),
            null,
            null
        );
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_NØDVENDIGHET);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.IKKE_VURDERT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom(),
            null);
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårSykdomsvilkårIkkeErOppfylt() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(journalpostId1, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).isEmpty();
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårGjennomgåttOpplæringIkkeErOppfylt() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(journalpostId1, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).isEmpty();
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårTidligereVilkårErDelvisOppfyltOgNødvendighetErGodkjent() {
        Periode godkjentPeriode = new Periode(søknadsperiode.getFom(), søknadsperiode.getTom().minusDays(1));
        Periode ikkeGodkjentPeriode = new Periode(søknadsperiode.getTom(), søknadsperiode.getTom());

        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.IKKE_OPPFYLT, ikkeGodkjentPeriode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, godkjentPeriode);

        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(journalpostId1, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpostId1, true, "", "", nå, List.of());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            null,
            new VurdertOpplæringHolder(List.of(vurdertOpplæring)),
            null,
            null
        );
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.OPPFYLT,
            godkjentPeriode.getFom(),
            godkjentPeriode.getTom(),
            null);
    }

    @Test
    public void skalIkkeLagreVilkårPeriodeUtenforSøknadsperiode() {
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT, søknadsperiode);
        scenario.leggTilVilkår(VilkårType.GJENNOMGÅ_OPPLÆRING, Utfall.OPPFYLT, søknadsperiode);
        behandling = scenario.lagre(repositoryProvider);
        setupUttakPerioder(journalpostId1, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(journalpostId1, true, "", "", nå, List.of());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            null,
            new VurdertOpplæringHolder(List.of(vurdertOpplæring)),
            null,
            null
        );
        lagreGrunnlag(grunnlag);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.OPPFYLT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom(),
            null);
    }

    private void assertVilkårPeriode(VilkårPeriode vilkårPeriode, Utfall utfall, LocalDate fom, LocalDate tom, Avslagsårsak avslagsårsak) {
        assertThat(vilkårPeriode.getUtfall()).isEqualTo(utfall);
        assertThat(vilkårPeriode.getFom()).isEqualTo(fom);
        assertThat(vilkårPeriode.getTom()).isEqualTo(tom);
        assertThat(vilkårPeriode.getAvslagsårsak()).isEqualTo(avslagsårsak);
    }
}
