package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
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

    @BeforeEach
    public void setup(){
        perioderTilVurderingTjenesteMock = spy(perioderTilVurderingTjenesteBean);
        vurdertOpplæringRepository = mock(VurdertOpplæringRepository.class);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderNødvendighetSteg = new VurderNødvendighetSteg(repositoryProvider, perioderTilVurderingTjenesteMock, vurdertOpplæringRepository);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);
        scenario.leggTilVilkår(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT);
        scenario.leggTilVilkår(VilkårType.NØDVENDIG_OPPLÆRING, Utfall.IKKE_VURDERT);
        behandling = scenario.lagre(repositoryProvider);
        LocalDate now = LocalDate.now();
        søknadsperiode = new Periode(now.minusMonths(3), now);
    }

    @Test
    public void skalReturnereAksjonspunktNårOpplæringIkkeErVurdert() {
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
        assertThat(vilkår.getPerioder().get(0).getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(vilkår.getPerioder().get(0).getFom()).isEqualTo(søknadsperiode.getFom());
        assertThat(vilkår.getPerioder().get(0).getTom()).isEqualTo(søknadsperiode.getTom());
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårOpplæringOgInstitusjonErGodkjent() {
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(), "fordi");
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon("Ås barnehage avdeling Vågebytoppen", true, "noe");
        grunnlag.medVurdertInstitusjon(Collections.singletonList(vurdertInstitusjon));
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(søknadsperiode.getFom(), søknadsperiode.getTom(), true, "", vurdertInstitusjon.getInstitusjon());
        grunnlag.medVurdertOpplæring(Collections.singletonList(vurdertOpplæring));
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(grunnlag));

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().get(0).getUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(vilkår.getPerioder().get(0).getFom()).isEqualTo(søknadsperiode.getFom());
        assertThat(vilkår.getPerioder().get(0).getTom()).isEqualTo(søknadsperiode.getTom());
    }

    @Test
    public void skalReturnereUtenAksjonspunktNårOpplæringIkkeErGodkjent() {
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(), "fordi");
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon("Xavier Institute", true, "noe");
        grunnlag.medVurdertInstitusjon(Collections.singletonList(vurdertInstitusjon));
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(søknadsperiode.getFom(), søknadsperiode.getTom(), false, "test", vurdertInstitusjon.getInstitusjon());
        grunnlag.medVurdertOpplæring(Collections.singletonList(vurdertOpplæring));
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(grunnlag));

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().get(0).getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkår.getPerioder().get(0).getFom()).isEqualTo(søknadsperiode.getFom());
        assertThat(vilkår.getPerioder().get(0).getTom()).isEqualTo(søknadsperiode.getTom());
    }

    @Test
    public void skalReturnereAksjonspunktNårVurderingIkkeErKomplett() {
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));
        setupPerioderTilVurdering(kontekst);

        VurdertOpplæringGrunnlag grunnlag = new VurdertOpplæringGrunnlag(behandling.getId(), "fordi");
        VurdertInstitusjon vurdertInstitusjon = new VurdertInstitusjon("Karoline Spiseri og Pub", true, "noe");
        grunnlag.medVurdertInstitusjon(Collections.singletonList(vurdertInstitusjon));
        VurdertOpplæring vurdertOpplæring = new VurdertOpplæring(søknadsperiode.getFom(), søknadsperiode.getTom().minusDays(1), true, "test", vurdertInstitusjon.getInstitusjon());
        grunnlag.medVurdertOpplæring(Collections.singletonList(vurdertOpplæring));
        when(vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(grunnlag));

        BehandleStegResultat resultat = vurderNødvendighetSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_INSTITUSJON_OG_NØDVENDIGHET);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.NØDVENDIG_OPPLÆRING).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().get(0).getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(vilkår.getPerioder().get(0).getFom()).isEqualTo(søknadsperiode.getFom());
        assertThat(vilkår.getPerioder().get(0).getTom()).isEqualTo(søknadsperiode.getTom());
    }

    private void setupPerioderTilVurdering(BehandlingskontrollKontekst kontekst) {
        when(perioderTilVurderingTjenesteMock.utled(kontekst.getBehandlingId(), VilkårType.NØDVENDIG_OPPLÆRING))
            .thenReturn(new TreeSet<>(Collections.singletonList(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFom(), søknadsperiode.getTom()))));
    }
}
