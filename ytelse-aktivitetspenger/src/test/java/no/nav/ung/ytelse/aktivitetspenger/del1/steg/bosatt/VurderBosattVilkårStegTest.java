package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriode;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriodeRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.bosatt.BostedAvklaringData;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderBosattVilkårStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    @Inject
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private AktivitetspengerSøktPeriodeRepository aktivitetspengerSøktPeriodeRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private VurderBosattVilkårSteg steg;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        bostedsGrunnlagRepository = new BostedsGrunnlagRepository(entityManager);
        aktivitetspengerSøktPeriodeRepository = new AktivitetspengerSøktPeriodeRepository(entityManager);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);

        var vilkårTjeneste = new VilkårTjeneste(behandlingRepository, vilkårsPerioderTilVurderingTjenester, vilkårResultatRepository);
        var tomEtterlysningTjeneste = new EtterlysningTjeneste(null, null) {
            @Override
            public List<EtterlysningData> hentGjeldendeEtterlysninger(Long behandlingId, Long fagsakId, no.nav.ung.kodeverk.varsel.EtterlysningType type) {
                return List.of();
            }
        };

        steg = new VurderBosattVilkårSteg(
            manuelleVilkårRekkefølgeTjeneste,
            vilkårResultatRepository,
            vilkårTjeneste,
            behandlingRepository,
            bostedsGrunnlagRepository,
            vilkårsPerioderTilVurderingTjenester,
            tomEtterlysningTjeneste
        );
    }

    @Test
    void skal_sette_oppfylt_og_regelinput_nar_bruker_er_bosatt_hele_perioden() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedsGrunnlagRepository.lagreAvklaringer(behandling.getId(), Map.of(
            FOM, new BostedAvklaringData(true, null, null, Kilde.SAKSBEHANDLER)
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var perioder = hentPerioder(behandling.getId());
        assertThat(perioder).hasSize(1);
        assertThat(perioder.getFirst().getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(perioder.getFirst().getRegelInput()).contains("\"skjaeringstidspunkt\"");
        assertThat(perioder.getFirst().getRegelInput()).contains("\"erBosattITrondheim\" : true");
    }

    @Test
    void skal_splitte_periode_ved_fraflytting_og_sette_avslag_med_regelinput() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var fraflyttingsDato = FOM.plusDays(10);
        bostedsGrunnlagRepository.lagreAvklaringer(behandling.getId(), Map.of(
            FOM, new BostedAvklaringData(true, fraflyttingsDato, FraflyttingsÅrsak.ANNET, Kilde.SAKSBEHANDLER)
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var perioder = hentPerioder(behandling.getId());
        assertThat(perioder).hasSize(2);

        var sortert = perioder.stream().sorted(Comparator.comparing(VilkårPeriode::getFom)).toList();
        assertThat(sortert.get(0).getFom()).isEqualTo(FOM);
        assertThat(sortert.get(0).getTom()).isEqualTo(fraflyttingsDato.minusDays(1));
        assertThat(sortert.get(0).getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);

        assertThat(sortert.get(1).getFom()).isEqualTo(fraflyttingsDato);
        assertThat(sortert.get(1).getTom()).isEqualTo(TOM);
        assertThat(sortert.get(1).getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(sortert.get(1).getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
        assertThat(sortert.get(1).getRegelInput()).contains("\"fraflyttingsAarsak\" : \"ANNET\"");
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        aktivitetspengerSøktPeriodeRepository.lagreNyPeriode(new AktivitetspengerSøktPeriode(
            behandling.getId(),
            new JournalpostId("jp-vilkår"),
            LocalDateTime.now(),
            periode));
        prosessTriggereRepository.leggTil(behandling.getId(), java.util.Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode)));
        return behandling;
    }

    private List<VilkårPeriode> hentPerioder(Long behandlingId) {
        return vilkårResultatRepository.hent(behandlingId)
            .getVilkår(VilkårType.BOSTEDSVILKÅR)
            .orElseThrow()
            .getPerioder();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}

