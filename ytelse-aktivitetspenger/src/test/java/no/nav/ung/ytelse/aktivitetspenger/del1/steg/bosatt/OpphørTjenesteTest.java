package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.bosatt.OpphørKilde;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.bosatt.OpphørResultat;
import no.nav.ung.sak.behandlingslager.bosatt.OpphørResultatRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class OpphørTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 3, 31);

    @Inject
    private EntityManager entityManager;

    private OpphørResultatRepository opphørResultatRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OpphørTjeneste opphørTjeneste;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        opphørResultatRepository = new OpphørResultatRepository(entityManager);
        opphørTjeneste = new OpphørTjeneste(opphørResultatRepository, vilkårResultatRepository);
    }

    @Test
    void utledOgLagreVilkår_med_null_opphørDato_setter_hele_perioden_oppfylt() {
        var behandling = opprettBehandling();
        opphørResultatRepository.lagre(new OpphørResultat(
            behandling.getId(), FOM, null, null, OpphørKilde.MANUELL, VilkårType.BOSTEDSVILKÅR));

        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, Boolean.TRUE)));
        opphørTjeneste.utledOgLagreVilkår(behandling.getId(), VilkårType.BOSTEDSVILKÅR, tidslinje);

        var perioder = hentPerioder(behandling.getId());
        assertThat(perioder).hasSize(1);
        assertThat(perioder.getFirst().getUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(perioder.getFirst().getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(perioder.getFirst().getPeriode().getTomDato()).isEqualTo(TOM);
    }

    @Test
    void utledOgLagreVilkår_med_opphørDato_innenfor_periode_splitter_oppfylt_og_ikke_oppfylt() {
        var behandling = opprettBehandling();
        var opphørDato = FOM.plusDays(15);
        opphørResultatRepository.lagre(new OpphørResultat(
            behandling.getId(), FOM, opphørDato, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, OpphørKilde.AUTOMATISK, VilkårType.BOSTEDSVILKÅR));

        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, Boolean.TRUE)));
        opphørTjeneste.utledOgLagreVilkår(behandling.getId(), VilkårType.BOSTEDSVILKÅR, tidslinje);

        var perioder = hentPerioder(behandling.getId());
        assertThat(perioder).hasSize(2);

        var oppfylt = perioder.stream().filter(p -> p.getUtfall() == Utfall.OPPFYLT).findFirst().orElseThrow();
        assertThat(oppfylt.getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(oppfylt.getPeriode().getTomDato()).isEqualTo(opphørDato.minusDays(1));

        var ikkeOppfylt = perioder.stream().filter(p -> p.getUtfall() == Utfall.IKKE_OPPFYLT).findFirst().orElseThrow();
        assertThat(ikkeOppfylt.getPeriode().getFomDato()).isEqualTo(opphørDato);
        assertThat(ikkeOppfylt.getPeriode().getTomDato()).isEqualTo(TOM);
        assertThat(ikkeOppfylt.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
    }

    @Test
    void utledOgLagreVilkår_med_opphørDato_etter_perioden_setter_hele_perioden_oppfylt() {
        var behandling = opprettBehandling();
        opphørResultatRepository.lagre(new OpphørResultat(
            behandling.getId(), FOM, TOM.plusDays(5), Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, OpphørKilde.AUTOMATISK, VilkårType.BOSTEDSVILKÅR));

        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, Boolean.TRUE)));
        opphørTjeneste.utledOgLagreVilkår(behandling.getId(), VilkårType.BOSTEDSVILKÅR, tidslinje);

        var perioder = hentPerioder(behandling.getId());
        assertThat(perioder).hasSize(1);
        assertThat(perioder.getFirst().getUtfall()).isEqualTo(Utfall.OPPFYLT);
    }

    @Test
    void utledOgLagreVilkår_med_opphørDato_før_stp_setter_hele_perioden_ikke_oppfylt() {
        var behandling = opprettBehandling();
        opphørResultatRepository.lagre(new OpphørResultat(
            behandling.getId(), FOM, FOM.minusDays(1), Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, OpphørKilde.AUTOMATISK, VilkårType.BOSTEDSVILKÅR));

        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, Boolean.TRUE)));
        opphørTjeneste.utledOgLagreVilkår(behandling.getId(), VilkårType.BOSTEDSVILKÅR, tidslinje);

        var perioder = hentPerioder(behandling.getId());
        assertThat(perioder).hasSize(1);
        assertThat(perioder.getFirst().getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
    }

    @Test
    void deaktiverOppfylteOpphørsresultater_deaktiverer_kun_der_opphørDato_overlapper() {
        var behandling = opprettBehandling();
        var opphørDatoInnenfor = FOM.plusDays(10);
        var opphørDatoUtenfor = TOM.plusDays(5);

        opphørResultatRepository.lagre(new OpphørResultat(
            behandling.getId(), FOM, opphørDatoInnenfor, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, OpphørKilde.AUTOMATISK, VilkårType.BOSTEDSVILKÅR));
        opphørResultatRepository.lagre(new OpphørResultat(
            behandling.getId(), TOM.plusDays(1), opphørDatoUtenfor, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, OpphørKilde.AUTOMATISK, VilkårType.BOSTEDSVILKÅR));

        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, Boolean.TRUE)));
        opphørTjeneste.deaktiverOppfylteOpphørsresultater(behandling.getId(), VilkårType.BOSTEDSVILKÅR, tidslinje);

        var aktive = opphørResultatRepository.hentAktiveForBehandling(behandling.getId());
        assertThat(aktive).hasSize(1);
        assertThat(aktive.getFirst().getOpphørDato()).isEqualTo(opphørDatoUtenfor);
    }

    @Test
    void deaktiverOppfylteOpphørsresultater_deaktiverer_ikke_resultat_med_null_opphørDato() {
        var behandling = opprettBehandling();
        opphørResultatRepository.lagre(new OpphørResultat(
            behandling.getId(), FOM, null, null, OpphørKilde.MANUELL, VilkårType.BOSTEDSVILKÅR));

        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, Boolean.TRUE)));
        opphørTjeneste.deaktiverOppfylteOpphørsresultater(behandling.getId(), VilkårType.BOSTEDSVILKÅR, tidslinje);

        var aktive = opphørResultatRepository.hentAktiveForBehandling(behandling.getId());
        assertThat(aktive).hasSize(1);
    }

    @Test
    void byggOpphørTidslinje_bygger_riktige_segmenter() {
        var stp1 = FOM;
        var stp2 = FOM.plusDays(15);
        var opphør1 = new OpphørResultat(1L, stp1, FOM.plusDays(5), Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, OpphørKilde.AUTOMATISK, VilkårType.BOSTEDSVILKÅR);
        var opphør2 = new OpphørResultat(1L, stp2, null, null, OpphørKilde.MANUELL, VilkårType.BOSTEDSVILKÅR);
        var map = Map.of(stp1, opphør1, stp2, opphør2);

        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(FOM, TOM, Boolean.TRUE)));
        var resultat = OpphørTjeneste.byggOpphørTidslinje(map, tidslinje);

        assertThat(resultat.toSegments()).hasSize(2);
        var segmenter = resultat.toSegments().stream().sorted((a, b) -> a.getFom().compareTo(b.getFom())).toList();
        assertThat(segmenter.get(0).getFom()).isEqualTo(stp1);
        assertThat(segmenter.get(0).getTom()).isEqualTo(stp2.minusDays(1));
        assertThat(segmenter.get(0).getValue()).isEqualTo(opphør1);
        assertThat(segmenter.get(1).getFom()).isEqualTo(stp2);
        assertThat(segmenter.get(1).getTom()).isEqualTo(TOM);
        assertThat(segmenter.get(1).getValue()).isEqualTo(opphør2);
    }

    @Test
    void byggOpphørTidslinje_returnerer_tom_tidslinje_ved_tom_input() {
        var resultat = OpphørTjeneste.byggOpphørTidslinje(Map.of(), LocalDateTimeline.empty());
        assertThat(resultat.isEmpty()).isTrue();
    }

    private Behandling opprettBehandling() {
        return AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .lagre(entityManager);
    }

    private List<VilkårPeriode> hentPerioder(Long behandlingId) {
        return vilkårResultatRepository.hent(behandlingId)
            .getVilkår(VilkårType.BOSTEDSVILKÅR)
            .orElseThrow()
            .getPerioder();
    }
}
