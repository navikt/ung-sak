package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
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
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;

@CdiDbAwareTest
public class VurderNødvendighetStegTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteBean;

    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteMock;
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    private Behandling behandling;
    private VurderNødvendighetSteg vurderNødvendighetSteg;
    private Periode søknadsperiode;
    private TestScenarioBuilder scenario;

    @BeforeEach
    public void setup(){
        perioderTilVurderingTjenesteMock = spy(perioderTilVurderingTjenesteBean);
        vurdertOpplæringRepository = mock(VurdertOpplæringRepository.class);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderNødvendighetSteg = new VurderNødvendighetSteg(repositoryProvider, perioderTilVurderingTjenesteMock, vurdertOpplæringRepository);
        LocalDate now = LocalDate.now();
        søknadsperiode = new Periode(now.minusMonths(3), now);
        scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);
        scenario.leggTilVilkår(VilkårType.NØDVENDIG_OPPLÆRING, Utfall.IKKE_VURDERT);
    }

    private void setupBehandlingMedSykdomsvilkår(Utfall utfall, Periode periode) {
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, utfall, periode);
        behandling = scenario.lagre(repositoryProvider);
    }

    private void setupPerioderTilVurdering(BehandlingskontrollKontekst kontekst) {
        when(perioderTilVurderingTjenesteMock.utled(kontekst.getBehandlingId(), VilkårType.NØDVENDIG_OPPLÆRING))
            .thenReturn(new TreeSet<>(Collections.singletonList(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFom(), søknadsperiode.getTom()))));
    }

    @Test
    public void skalReturnereAksjonspunktNårOpplæringIkkeErVurdert() {
        setupBehandlingMedSykdomsvilkår(Utfall.OPPFYLT, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_INSTITUSJON_OG_NØDVENDIGHET);
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
    public void skalReturnereUtenAksjonspunktNårOpplæringOgInstitusjonErGodkjent() {
        setupBehandlingMedSykdomsvilkår(Utfall.OPPFYLT, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon("Ås barnehage avdeling Vågebytoppen", true, "noe");
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(søknadsperiode.getFom(), søknadsperiode.getTom(), true, "", vurdertInstitusjon.getInstitusjon());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            new VurdertInstitusjonHolder(Collections.singletonList(vurdertInstitusjon)),
            new VurdertOpplæringHolder(Collections.singletonList(vurdertOpplæring)),
            "fordi");
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(grunnlag));

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

    @Test
    public void skalReturnereUtenAksjonspunktNårOpplæringIkkeErGodkjent() {
        setupBehandlingMedSykdomsvilkår(Utfall.OPPFYLT, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon("Xavier Institute", true, "noe");
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(søknadsperiode.getFom(), søknadsperiode.getTom(), false, "test", vurdertInstitusjon.getInstitusjon());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            new VurdertInstitusjonHolder(Collections.singletonList(vurdertInstitusjon)),
            new VurdertOpplæringHolder(Collections.singletonList(vurdertOpplæring)),
            "fordi");
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(grunnlag));

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
            Avslagsårsak.IKKE_NØDVENDIG);
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårInstitusjonIkkeErGodkjent() {
        setupBehandlingMedSykdomsvilkår(Utfall.OPPFYLT, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon("Riskhospitalet", false, "noe");
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(søknadsperiode.getFom(), søknadsperiode.getTom(), false, "test", vurdertInstitusjon.getInstitusjon());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            new VurdertInstitusjonHolder(Collections.singletonList(vurdertInstitusjon)),
            new VurdertOpplæringHolder(Collections.singletonList(vurdertOpplæring)),
            "fordi");
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(grunnlag));

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
            Avslagsårsak.IKKE_GODKJENT_INSTITUSJON);
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårOpplæringErPeriodevisGodkjentOgIkkeGodkjent() {
        setupBehandlingMedSykdomsvilkår(Utfall.OPPFYLT, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon("Sjøengen Kro", true, "noe");
        VurdertOpplæring vurdertOpplæring1 = new VurdertOpplæring(søknadsperiode.getFom(), søknadsperiode.getTom().minusDays(1), true, "test", vurdertInstitusjon.getInstitusjon());
        VurdertOpplæring vurdertOpplæring2 = new VurdertOpplæring(søknadsperiode.getTom(), søknadsperiode.getTom(), false, "tast", vurdertInstitusjon.getInstitusjon());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            new VurdertInstitusjonHolder(Collections.singletonList(vurdertInstitusjon)),
            new VurdertOpplæringHolder(Arrays.asList(vurdertOpplæring1, vurdertOpplæring2)),
            "fordi");
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(grunnlag));

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
            Avslagsårsak.IKKE_NØDVENDIG);
    }

    @Test
    public void skalReturnereAksjonspunktNårVurderingIkkeErKomplett() {
        setupBehandlingMedSykdomsvilkår(Utfall.OPPFYLT, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon("Karoline Spiseri og Pub", true, "noe");
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(søknadsperiode.getFom(), søknadsperiode.getTom().minusDays(1), true, "test", vurdertInstitusjon.getInstitusjon());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            new VurdertInstitusjonHolder(Collections.singletonList(vurdertInstitusjon)),
            new VurdertOpplæringHolder(Collections.singletonList(vurdertOpplæring)),
            "fordi");
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(grunnlag));

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_INSTITUSJON_OG_NØDVENDIGHET);
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
        setupBehandlingMedSykdomsvilkår(Utfall.IKKE_OPPFYLT, søknadsperiode);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

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
            Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE);
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårSykdomsvilkårErDelvisOppfyltOgOpplæringErGodkjent() {
        setupBehandlingMedSykdomsvilkår(Utfall.OPPFYLT, new Periode(søknadsperiode.getFom(), søknadsperiode.getTom().minusMonths(1)));

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon("der", true, "noe");
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(søknadsperiode.getFom(), søknadsperiode.getTom().minusMonths(1), true, "", vurdertInstitusjon.getInstitusjon());
        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(),
            new VurdertInstitusjonHolder(Collections.singletonList(vurdertInstitusjon)),
            new VurdertOpplæringHolder(Collections.singletonList(vurdertOpplæring)),
            "fordi");
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(grunnlag));

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.OPPFYLT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom().minusMonths(1),
            null);
        assertVilkårPeriode(vilkår.getPerioder().get(1),
            Utfall.IKKE_OPPFYLT,
            søknadsperiode.getTom().minusMonths(1).plusDays(1),
            søknadsperiode.getTom(),
            Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE);
    }

    private void assertVilkårPeriode(VilkårPeriode vilkårPeriode, Utfall utfall, LocalDate fom, LocalDate tom, Avslagsårsak avslagsårsak) {
        assertThat(vilkårPeriode.getUtfall()).isEqualTo(utfall);
        assertThat(vilkårPeriode.getFom()).isEqualTo(fom);
        assertThat(vilkårPeriode.getTom()).isEqualTo(tom);
        assertThat(vilkårPeriode.getAvslagsårsak()).isEqualTo(avslagsårsak);
    }
}
