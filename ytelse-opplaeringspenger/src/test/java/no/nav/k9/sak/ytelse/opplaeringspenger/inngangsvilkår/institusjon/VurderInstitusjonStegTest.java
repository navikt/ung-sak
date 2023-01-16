package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

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
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjonPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;

@CdiDbAwareTest
class VurderInstitusjonStegTest {

    private VurderInstitusjonSteg vurderInstitusjonSteg;

    @Inject
    private EntityManager entityManager;

    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;

    @Inject
    private GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;

    @Inject
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteBean;

    private Behandling behandling;
    private Periode søknadsperiode;
    private final JournalpostId journalpostId = new JournalpostId("123");
    private final UUID institusjonUuid = UUID.randomUUID();
    private BehandlingskontrollKontekst kontekst;

    @BeforeEach
    void setup() {
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenesteMock = spy(perioderTilVurderingTjenesteBean);
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderInstitusjonSteg = new VurderInstitusjonSteg(repositoryProvider,
            new VurderInstitusjonTjeneste(repositoryProvider, godkjentOpplæringsinstitusjonTjeneste, perioderTilVurderingTjenesteMock, vurdertOpplæringRepository, uttakPerioderGrunnlagRepository));

        LocalDate fom = LocalDate.now().minusMonths(3);
        LocalDate tom = LocalDate.now();
        søknadsperiode = new Periode(fom, tom);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);
        scenario.leggTilVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.IKKE_VURDERT);
        scenario.leggTilVilkår(VilkårType.NØDVENDIG_OPPLÆRING, Utfall.IKKE_VURDERT);
        behandling = scenario.lagre(repositoryProvider);
        Fagsak fagsak = behandling.getFagsak();
        kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));

        when(perioderTilVurderingTjenesteMock.utled(kontekst.getBehandlingId(), VilkårType.NØDVENDIG_OPPLÆRING))
            .thenReturn(new TreeSet<>(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))));

        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), Duration.ofHours(7).plusMinutes(30))),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new KursPeriode(fom, tom, null, null, "institusjon", institusjonUuid)));
        uttakPerioderGrunnlagRepository.lagre(behandling.getId(), perioderFraSøknad);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));
    }

    @Test
    void skalReturnereAksjonspunktNårDetManglerVurdering() {
        BehandleStegResultat resultat = vurderInstitusjonSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_INSTITUSJON);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON).orElse(null);
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
        VurdertInstitusjonHolder vurdertInstitusjonHolder = new VurdertInstitusjonHolder(List.of(new VurdertInstitusjon(journalpostId, true, "jo")));
        vurdertOpplæringRepository.lagre(behandling.getId(), vurdertInstitusjonHolder);

        BehandleStegResultat resultat = vurderInstitusjonSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(0);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON).orElse(null);
        assertThat(vilkår).isNotNull();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertVilkårPeriode(vilkår.getPerioder().get(0),
            Utfall.OPPFYLT,
            søknadsperiode.getFom(),
            søknadsperiode.getTom(),
            null);
    }

    @Test
    void skalReturnereUtenAksjonspunktMedInstitusjonIRegister() {
        GodkjentOpplæringsinstitusjon godkjentOpplæringsinstitusjon = new GodkjentOpplæringsinstitusjon(institusjonUuid, "institusjon", List.of(new GodkjentOpplæringsinstitusjonPeriode(søknadsperiode.getFom(), søknadsperiode.getTom())));
        entityManager.persist(godkjentOpplæringsinstitusjon);
        entityManager.flush();

        BehandleStegResultat resultat = vurderInstitusjonSteg.utførSteg(kontekst);
        assertThat(resultat).isNotNull();
        assertThat(resultat.getAksjonspunktResultater()).hasSize(0);
        Vilkår vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON).orElse(null);
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
