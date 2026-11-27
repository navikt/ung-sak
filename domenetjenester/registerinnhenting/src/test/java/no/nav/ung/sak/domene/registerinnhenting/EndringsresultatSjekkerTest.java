package no.nav.ung.sak.domene.registerinnhenting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårsResultat;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPerioder;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsResultat;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class EndringsresultatSjekkerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 9, 30);
    private static final DatoIntervallEntitet AUGUST = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, LocalDate.of(2025, 8, 31));

    @Inject
    private EntityManager entityManager;

    @Any
    @Inject
    private Instance<EndringStartpunktUtleder> startpunktUtledere;

    @Any
    @Inject
    private Instance<SøknadDokumentTjeneste> søknadDokumentTjenester;

    private VilkårResultatRepository vilkårResultatRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private EndringsresultatSjekker sjekker;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);

        // Uten registerdata, så snapshotet ikke trenger abakus
        var informasjonselementer = mock(InformasjonselementerUtleder.class);
        when(informasjonselementer.utled(any())).thenReturn(Set.of());

        sjekker = new EndringsresultatSjekker(
            new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository()),
            new UnitTestLookupInstanceImpl<>(informasjonselementer),
            startpunktUtledere,
            søknadDokumentTjenester,
            mock(InntektArbeidYtelseTjeneste.class),
            new ProsessTriggereRepository(entityManager),
            new UngdomsprogramPeriodeRepository(entityManager),
            repositoryProvider.getBehandlingRepository(),
            vilkårResultatRepository,
            tilkjentYtelseRepository,
            ungdomsytelseGrunnlagRepository);

        behandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
        lagreVilkår(Utfall.OPPFYLT);
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), new UngdomsytelseSatsResultat(new LocalDateTimeline<>(FOM, TOM,
            new UngdomsytelseSatser(BigDecimal.TEN, BigDecimal.valueOf(130_000), BigDecimal.valueOf(2), UngdomsytelseSatsType.HØY, 0, 0)), "regelInput", "regelSporing"));
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), uttak(null));
    }

    @Test
    void utvidet_snapshot_fra_task_payload_fanger_endringer_fra_synk_og_gir_vurder_kompletthet() throws IOException {
        var snapshotFør = viaTaskPayload(sjekker.opprettUtvidetEndringsresultatSnapshot(behandling.getId()));

        endreAggregateneSomSynkenErstatter();
        var diff = sjekker.finnSporedeEndringerPåBehandlingsgrunnlag(behandling.getId(), snapshotFør);

        assertThat(erEndret(diff, VilkårsResultat.class)).isTrue();
        assertThat(erEndret(diff, KontrollertInntektPerioder.class)).isTrue();
        assertThat(erEndret(diff, UngdomsytelseGrunnlag.class)).isTrue();
        assertThat(new EndringStartpunktTjeneste(startpunktUtledere).utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse.fra(behandling), diff))
            .isEqualTo(StartpunktType.VURDER_KOMPLETTHET);
    }

    @Test
    void vanlig_snapshot_tar_ikke_med_aggregatene_som_synken_erstatter() throws IOException {
        var snapshotFør = viaTaskPayload(sjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandling.getId()));

        endreAggregateneSomSynkenErstatter();
        var diff = sjekker.finnSporedeEndringerPåBehandlingsgrunnlag(behandling.getId(), snapshotFør);

        assertThat(diff.hentDelresultat(VilkårsResultat.class)).isEmpty();
        assertThat(diff.hentDelresultat(KontrollertInntektPerioder.class)).isEmpty();
        assertThat(diff.hentDelresultat(UngdomsytelseGrunnlag.class)).isEmpty();
    }

    private void endreAggregateneSomSynkenErstatter() {
        lagreVilkår(Utfall.IKKE_OPPFYLT);
        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(KontrollertInntektPeriode.ny()
            .medPeriode(AUGUST)
            .medInntekt(BigDecimal.TEN)
            .medKilde(KontrollertInntektKilde.REGISTER)
            .build()));
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), uttak(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER));
    }

    private void lagreVilkår(Utfall utfall) {
        var builder = Vilkårene.builder();
        var vilkår = builder.hentBuilderFor(VilkårType.UNGDOMSPROGRAMVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder().medPeriode(FOM, TOM).medUtfall(utfall));
        vilkårResultatRepository.lagre(behandling.getId(), builder.leggTil(vilkår).build());
    }

    private static UngdomsytelseUttakPerioder uttak(UngdomsytelseUttakAvslagsårsak avslagsårsak) {
        var uttak = new UngdomsytelseUttakPerioder(List.of(new UngdomsytelseUttakPeriode(FOM, TOM, avslagsårsak)));
        uttak.setRegelInput("input");
        uttak.setRegelSporing("sporing");
        return uttak;
    }

    private static EndringsresultatSnapshot viaTaskPayload(EndringsresultatSnapshot snapshot) throws IOException {
        return JsonObjectMapper.fromJson(JsonObjectMapper.getJson(snapshot), EndringsresultatSnapshot.class);
    }

    private static boolean erEndret(EndringsresultatDiff diff, Class<?> aggregat) {
        return diff.hentDelresultat(aggregat).map(EndringsresultatDiff::erSporedeFeltEndret).orElse(false);
    }
}
