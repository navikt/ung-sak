package no.nav.ung.sak.inngangsvilkår.ungdomsprogram;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.*;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_UNGDOMSPROGRAMVILKÅR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VurderUngdomsprogramVilkårStegTest {

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    @BehandlingStegRef(value = VURDER_UNGDOMSPROGRAMVILKÅR)
    @BehandlingTypeRef
    @FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
    private VurderUngdomsprogramVilkårSteg vurderUngdomsprogramVilkårSteg;

    private Behandling behandling;


    @BeforeEach
    void setUp() {
        lagFagsakOgBehandling(LocalDate.now().minusMonths(6));
    }

    @Test
    void skal_sette_hele_periode_til_innvilget() {
        // Arrange
        final var fom = LocalDate.now();
        final var tom = LocalDate.now().plusDays(10);
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        initierVilkår(periode);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(
                new UngdomsprogramPeriode(periode)
        ));

        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, periode)
        ));

        // Act
        vurderUngdomsprogramVilkårSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));

        // Assert
        final var resultatVilkår = vilkårResultatRepository.hent(behandling.getId());
        final var resultatperioder = resultatVilkår.getVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET).stream().map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .toList();
        assertThat(resultatperioder.size()).isEqualTo(1);

        final var resultatPeriode = resultatperioder.get(0);
        assertThat(resultatPeriode.getPeriode()).isEqualTo(periode);
        assertThat(resultatPeriode.getUtfall()).isEqualTo(Utfall.OPPFYLT);
    }

    @Test
    void skal_avslå_del_i_forkant_med_endret_startdato_årsak() {
        // Arrange
        final var opprinneligFom = LocalDate.now();
        final var nyFom = LocalDate.now().plusDays(5);
        final var tom = LocalDate.now().plusDays(10);
        final var opprinneligPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(opprinneligFom, tom);
        final var nyPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(nyFom, tom);
        initierVilkår(opprinneligPeriode);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(
                new UngdomsprogramPeriode(nyPeriode)
        ));
        final var endretPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(opprinneligFom, nyFom.minusDays(1));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, endretPeriode)
        ));

        // Act
        vurderUngdomsprogramVilkårSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));

        // Assert
        final var resultatVilkår = vilkårResultatRepository.hent(behandling.getId());
        final var resultatperioder = resultatVilkår.getVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET).stream().map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .toList();
        assertThat(resultatperioder.size()).isEqualTo(2);

        final var førstePeriode = resultatperioder.stream().filter(it -> it.getPeriode().equals(endretPeriode)).findFirst().get();
        assertThat(førstePeriode.getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(førstePeriode.getAvslagsårsak()).isEqualTo(Avslagsårsak.ENDRET_STARTDATO_UNGDOMSPROGRAM);

        final var andrePeriode = resultatperioder.stream().filter(it -> it.getPeriode().equals(nyPeriode)).findFirst().get();
        assertThat(andrePeriode.getUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(andrePeriode.getAvslagsårsak()).isNull();
    }

    @Test
    void skal_avslå_del_i_etterkant_med_opphør_årsak() {
        // Arrange
        final var fom = LocalDate.now();
        final var opprinneligTom = LocalDate.now().plusDays(10);
        final var nyTom = LocalDate.now().plusDays(5);
        final var opprinneligPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, opprinneligTom);
        final var nyPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, nyTom);
        initierVilkår(opprinneligPeriode);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(
                new UngdomsprogramPeriode(nyPeriode)
        ));
        final var endretPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(nyTom.plusDays(1), opprinneligTom);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, endretPeriode)
        ));

        // Act
        vurderUngdomsprogramVilkårSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));

        // Assert
        final var resultatVilkår = vilkårResultatRepository.hent(behandling.getId());
        final var resultatperioder = resultatVilkår.getVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET).stream().map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .toList();
        assertThat(resultatperioder.size()).isEqualTo(2);

        final var avslåttPeriode = resultatperioder.stream().filter(it -> it.getPeriode().equals(endretPeriode)).findFirst().get();
        assertThat(avslåttPeriode.getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(avslåttPeriode.getAvslagsårsak()).isEqualTo(Avslagsårsak.OPPHØRT_UNGDOMSPROGRAM);

        final var innvilgetPeriode = resultatperioder.stream().filter(it -> it.getPeriode().equals(nyPeriode)).findFirst().get();
        assertThat(innvilgetPeriode.getUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(innvilgetPeriode.getAvslagsårsak()).isNull();
    }


    private void initierVilkår(DatoIntervallEntitet periode) {
        final var vilkåreneBuilder = Vilkårene.builder();
        vilkåreneBuilder.leggTilIkkeVurderteVilkår(
            Map.of(VilkårType.UNGDOMSPROGRAMVILKÅRET, new TreeSet<>(Set.of(periode))), new TreeSet<>());
        vilkårResultatRepository.lagre(behandling.getId(), vilkåreneBuilder.build());
    }

    private Long lagFagsakOgBehandling(LocalDate fom) {
        final var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, fom.plusWeeks(52));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling.getId();
    }

}
