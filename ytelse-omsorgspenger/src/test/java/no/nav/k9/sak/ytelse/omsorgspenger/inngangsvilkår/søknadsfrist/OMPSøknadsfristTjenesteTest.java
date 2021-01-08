package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.Søknad;
import no.nav.k9.sak.perioder.SøknadType;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class OMPSøknadsfristTjenesteTest {

    private OMPSøknadsfristTjeneste tjeneste = new OMPSøknadsfristTjeneste(null);

    @Test
    void skal_mappe_til_vilkårsresultat() {

        var søktPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now());
        var vurdertPeriode = new VurdertSøktPeriode<OppgittFraværPeriode>(søktPeriode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef(), Utfall.OPPFYLT, null);
        Map<Søknad, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurdertePerioder = Map.of(
            new Søknad(new JournalpostId(123L), LocalDateTime.now(), SøknadType.INNTEKTSMELDING),
            List.of(vurdertPeriode));

        VilkårResultatBuilder resultatBuilder = tjeneste.mapVurderingerTilVilkårsresultat(Vilkårene.builder(), Map.of(), vurdertePerioder, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(12), LocalDate.now().plusMonths(12)));

        var vilkårene = resultatBuilder.build();

        var vilkår = vilkårene.getVilkår(VilkårType.SØKNADSFRIST).orElseThrow();
        assertThat(vilkår.getPerioder()).isNotEmpty();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().get(0).getUtfall()).isEqualTo(Utfall.OPPFYLT);
    }

    @Test
    void skal_prioriterer_til_vurdering() {

        var søktPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now());
        var vurdertPeriode = new VurdertSøktPeriode<OppgittFraværPeriode>(søktPeriode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef(), Utfall.OPPFYLT, null);
        var vurdertPeriode1 = new VurdertSøktPeriode<OppgittFraværPeriode>(søktPeriode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000001"), InternArbeidsforholdRef.nullRef(), Utfall.IKKE_VURDERT, null);
        Map<Søknad, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurdertePerioder = Map.of(
            new Søknad(new JournalpostId(123L), LocalDateTime.now(), SøknadType.INNTEKTSMELDING),
            List.of(vurdertPeriode),
            new Søknad(new JournalpostId(124L), LocalDateTime.now(), SøknadType.INNTEKTSMELDING),
            List.of(vurdertPeriode1));

        VilkårResultatBuilder resultatBuilder = tjeneste.mapVurderingerTilVilkårsresultat(Vilkårene.builder(), Map.of(), vurdertePerioder, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(12), LocalDate.now().plusMonths(12)));

        var vilkårene = resultatBuilder.build();

        var vilkår = vilkårene.getVilkår(VilkårType.SØKNADSFRIST).orElseThrow();
        assertThat(vilkår.getPerioder()).isNotEmpty();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().get(0).getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_prioriterer_til_oppfylt_over_avslag() {

        var søktPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now());
        var vurdertPeriode = new VurdertSøktPeriode<OppgittFraværPeriode>(søktPeriode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), InternArbeidsforholdRef.nullRef(), Utfall.OPPFYLT, null);
        var vurdertPeriode1 = new VurdertSøktPeriode<OppgittFraværPeriode>(søktPeriode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000001"), InternArbeidsforholdRef.nullRef(), Utfall.IKKE_OPPFYLT, null);
        Map<Søknad, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurdertePerioder = Map.of(
            new Søknad(new JournalpostId(123L), LocalDateTime.now(), SøknadType.INNTEKTSMELDING),
            List.of(vurdertPeriode),
            new Søknad(new JournalpostId(124L), LocalDateTime.now(), SøknadType.INNTEKTSMELDING),
            List.of(vurdertPeriode1));

        VilkårResultatBuilder resultatBuilder = tjeneste.mapVurderingerTilVilkårsresultat(Vilkårene.builder(), Map.of(), vurdertePerioder, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(12), LocalDate.now().plusMonths(12)));

        var vilkårene = resultatBuilder.build();

        var vilkår = vilkårene.getVilkår(VilkårType.SØKNADSFRIST).orElseThrow();
        assertThat(vilkår.getPerioder()).isNotEmpty();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().get(0).getUtfall()).isEqualTo(Utfall.OPPFYLT);
    }
}
