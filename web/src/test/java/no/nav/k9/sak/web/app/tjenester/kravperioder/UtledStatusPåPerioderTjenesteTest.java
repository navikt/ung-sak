package no.nav.k9.sak.web.app.tjenester.kravperioder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.krav.PeriodeMedÅrsaker;
import no.nav.k9.sak.kontrakt.krav.RolleType;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.k9.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;

class UtledStatusPåPerioderTjenesteTest {


    private final UtledStatusPåPerioderTjeneste utledStatusPåPerioderTjeneste = new UtledStatusPåPerioderTjeneste(false);
    private final LocalDate IDAG = LocalDate.now();
    private final LocalDateTime NÅ = LocalDateTime.now();
    private final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.virksomhet("000000000");
    private final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.virksomhet("000000001");

    @Test
    void samme_inntektsmelding_revurdering() {
        var førstegangsscenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMSORGSPENGER);
        Behandling behandling = førstegangsscenario
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();


        KravDokument kravDokTilkommetBehandling = new KravDokument(new JournalpostId("1"), NÅ, KravDokumentType.INNTEKTSMELDING);
        KravDokument kravDokTidligereBehandling = new KravDokument(new JournalpostId("2"), NÅ, KravDokumentType.INNTEKTSMELDING);

        LocalDate fom = IDAG.minusMonths(1);
        LocalDate tom = IDAG;

        var kravdokumenterMedPeriode = Map.of(
            kravDokTilkommetBehandling, List.of(byggSøktPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), ARBEIDSGIVER1)),
            kravDokTidligereBehandling, List.of(byggSøktPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
        );

        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fra(fom, tom)));

        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = new TreeSet<>();

        var revurderingPerioderFraAndreParter = revurderingPerioderFraAndreParter(fom, tom, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        StatusForPerioderPåBehandling utled = utledStatusPåPerioderTjeneste.utled(
            behandling,
            new PåTversAvHelgErKantIKantVurderer(),
            Set.of(kravDokTilkommetBehandling),
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter
        );

        var perioderMedÅrsakPerKravstiller = utled.getPerioderMedÅrsakPerKravstiller();
        assertThat(perioderMedÅrsakPerKravstiller).hasSize(1);
        assertThat(perioderMedÅrsakPerKravstiller.get(0).kravstiller()).isEqualTo(RolleType.ARBEIDSGIVER);
        assertThat(perioderMedÅrsakPerKravstiller.get(0).arbeidsgiver()).isEqualTo(ARBEIDSGIVER1);
        assertThat(perioderMedÅrsakPerKravstiller.get(0).perioderMedÅrsak()
            .stream().findFirst().get().getÅrsaker()).containsOnly(ÅrsakTilVurdering.REVURDERER_NY_INNTEKTSMELDING);

    }

    @Test
    void samme_inntektsmelding_revurdering_flere_arbeidsgivere() {
        var førstegangsscenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMSORGSPENGER);
        Behandling behandling = førstegangsscenario
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();


        KravDokument kravDokTilkommetBehandling = new KravDokument(new JournalpostId("1"), NÅ, KravDokumentType.INNTEKTSMELDING);
        KravDokument kravDokTidligereBehandling = new KravDokument(new JournalpostId("2"), NÅ, KravDokumentType.INNTEKTSMELDING);

        LocalDate fom = IDAG.minusMonths(1);
        LocalDate tom = IDAG;

        var kravdokumenterMedPeriode = Map.of(
            kravDokTilkommetBehandling, List.of(byggSøktPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), ARBEIDSGIVER1)),
            kravDokTidligereBehandling, List.of(byggSøktPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
        );

        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fra(fom, tom)));

        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = new TreeSet<>();

        var revurderingPerioderFraAndreParter = revurderingPerioderFraAndreParter(fom, tom, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        StatusForPerioderPåBehandling utled = utledStatusPåPerioderTjeneste.utled(
            behandling,
            new PåTversAvHelgErKantIKantVurderer(),
            Set.of(kravDokTilkommetBehandling),
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter
        );

        var perioderMedÅrsakPerKravstiller = utled.getPerioderMedÅrsakPerKravstiller();
        assertThat(perioderMedÅrsakPerKravstiller).hasSize(1);
        assertThat(perioderMedÅrsakPerKravstiller.get(0).kravstiller()).isEqualTo(RolleType.ARBEIDSGIVER);
        assertThat(perioderMedÅrsakPerKravstiller.get(0).arbeidsgiver()).isEqualTo(ARBEIDSGIVER1);
        assertThat(perioderMedÅrsakPerKravstiller.get(0).perioderMedÅrsak()
            .stream().findFirst().get().getÅrsaker()).containsOnly(ÅrsakTilVurdering.REVURDERER_NY_INNTEKTSMELDING);

    }

    @Test
    void samme_søknad_inntektsmelding_revurdering() {
        var førstegangsscenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        Behandling behandling = førstegangsscenario
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();


        KravDokument søknadTilkommetBehandling = new KravDokument(new JournalpostId("1"), NÅ, KravDokumentType.SØKNAD);
        KravDokument inntektsmeldingTidligereBehandling = new KravDokument(new JournalpostId("3"), NÅ, KravDokumentType.INNTEKTSMELDING);


        LocalDate fom = IDAG.minusMonths(1);
        LocalDate tom = IDAG;

        var kravdokumenterMedPeriode = Map.of(
            søknadTilkommetBehandling, List.of(byggSøktPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))),
            inntektsmeldingTidligereBehandling, List.of(byggSøktPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), ARBEIDSGIVER1))
        );

        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fra(fom, tom)));

        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = new TreeSet<>();

        NavigableSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter = new TreeSet<>();

        StatusForPerioderPåBehandling utled = utledStatusPåPerioderTjeneste.utled(
            behandling,
            new PåTversAvHelgErKantIKantVurderer(),
            Set.of(søknadTilkommetBehandling),
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter
        );

        assertThat(utled.getPerioderMedÅrsak().stream().findFirst().get().getÅrsaker()).containsOnly(ÅrsakTilVurdering.ENDRING_FRA_BRUKER);
    }


    @Test
    void perioderMedÅrsakPerKravStiller_omp_bruker_førstegangsvurdert() {
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMSORGSPENGER);
        Behandling behandling = scenario
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();


        KravDokument kravDokTilkommetBehandling1 = new KravDokument(new JournalpostId("1"), NÅ, KravDokumentType.SØKNAD);
        KravDokument kravDokTilkommetBehandling2 = new KravDokument(new JournalpostId("2"), NÅ, KravDokumentType.SØKNAD);
        KravDokument kravDokTidligereBehandling = new KravDokument(new JournalpostId("3"), NÅ, KravDokumentType.SØKNAD);

        LocalDate fom = IDAG.minusMonths(1);
        LocalDate tom = IDAG;

        DatoIntervallEntitet tilkommetPeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        DatoIntervallEntitet tilkommetPeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusWeeks(1), tom);
        DatoIntervallEntitet tidligerePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusMonths(2), tom.minusMonths(2));

        var kravdokumenterMedPeriode = Map.of(
            kravDokTilkommetBehandling1, List.of(byggSøktPeriode(tilkommetPeriode1)),
            kravDokTilkommetBehandling2, List.of(byggSøktPeriode(tilkommetPeriode2)),
            kravDokTidligereBehandling, List.of(byggSøktPeriode(tidligerePeriode))
        );

        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fra(fom, tom)));

        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = new TreeSet<>();

        var revurderingPerioderFraAndreParter = revurderingPerioderFraAndreParter(fom, tom, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        StatusForPerioderPåBehandling svar = utledStatusPåPerioderTjeneste.utled(
            behandling,
            new PåTversAvHelgErKantIKantVurderer(),
            Set.of(kravDokTilkommetBehandling1, kravDokTilkommetBehandling2),
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter
        );


        assertThat(svar.getPerioderMedÅrsak().get(0).getPeriode()).isEqualTo(new Periode(fom, tom));
        assertThat(svar.getPerioderMedÅrsak().get(0).getÅrsaker()).containsOnly(ÅrsakTilVurdering.FØRSTEGANGSVURDERING);

        var perioderMedÅrsakPerKravstiller = svar.getPerioderMedÅrsakPerKravstiller();
        assertThat(perioderMedÅrsakPerKravstiller).hasSize(1);
        assertThat(perioderMedÅrsakPerKravstiller.get(0).kravstiller()).isEqualTo(RolleType.BRUKER);
        var perioderMedÅrsak = perioderMedÅrsakPerKravstiller.get(0).perioderMedÅrsak();
        assertThat(perioderMedÅrsak).containsExactlyInAnyOrderElementsOf(svar.getPerioderMedÅrsak());

    }

    @Test
    void perioderMedÅrsakPerKravStiller_psb_bruker_endring() {
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        Behandling behandling = scenario
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();


        KravDokument kravDokTilkommetBehandling = new KravDokument(new JournalpostId("1"), NÅ, KravDokumentType.SØKNAD);
        KravDokument kravDokTidligereBehandling1 = new KravDokument(new JournalpostId("2"), NÅ, KravDokumentType.SØKNAD);
        KravDokument kravDokTidligereBehandling2 = new KravDokument(new JournalpostId("3"), NÅ, KravDokumentType.SØKNAD);

        LocalDate fom = IDAG.minusMonths(1);
        LocalDate tom = IDAG;

        DatoIntervallEntitet tilkommetPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        DatoIntervallEntitet tidligerePeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusWeeks(1), tom);
        DatoIntervallEntitet tidligerePeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusMonths(2), tom.minusMonths(2));

        var kravdokumenterMedPeriode = Map.of(
            kravDokTilkommetBehandling, List.of(byggSøktPeriode(tilkommetPeriode)),
            kravDokTidligereBehandling1, List.of(byggSøktPeriode(tidligerePeriode1)),
            kravDokTidligereBehandling2, List.of(byggSøktPeriode(tidligerePeriode2))
        );

        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fra(fom, tom)));

        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = new TreeSet<>();

        NavigableSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter = new TreeSet<>();

        StatusForPerioderPåBehandling svar = utledStatusPåPerioderTjeneste.utled(
            behandling,
            new PåTversAvHelgErKantIKantVurderer(),
            Set.of(kravDokTilkommetBehandling),
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter
        );


        assertThat(svar.getPerioderMedÅrsak()).hasSize(2);

        Periode førstegangsvurdering = new Periode(fom, tidligerePeriode1.getFomDato().minusDays(1));
        Periode endring = tidligerePeriode1.tilPeriode();

        assertThat(svar.getPerioderMedÅrsak()).extracting(
            PeriodeMedÅrsaker::getPeriode, PeriodeMedÅrsaker::getÅrsaker
        ).contains(
            tuple(endring, Set.of(ÅrsakTilVurdering.ENDRING_FRA_BRUKER)),
            tuple(førstegangsvurdering, Set.of(ÅrsakTilVurdering.FØRSTEGANGSVURDERING))
        );


        var perioderMedÅrsakPerKravstiller = svar.getPerioderMedÅrsakPerKravstiller();
        assertThat(perioderMedÅrsakPerKravstiller).hasSize(1);
        assertThat(perioderMedÅrsakPerKravstiller.get(0).kravstiller()).isEqualTo(RolleType.BRUKER);
        var perioderMedÅrsak = perioderMedÅrsakPerKravstiller.get(0).perioderMedÅrsak();
        assertThat(perioderMedÅrsak).containsExactlyInAnyOrderElementsOf(svar.getPerioderMedÅrsak());
    }

    private static TreeSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter(LocalDate fom, LocalDate tom, BehandlingÅrsakType reEndretInntektsmelding) {
        return new TreeSet<>(Set.of(new PeriodeMedÅrsak(DatoIntervallEntitet.fra(fom, tom), reEndretInntektsmelding)));
    }

    private SøktPeriode<VurdertSøktPeriode.SøktPeriodeData> byggSøktPeriode(DatoIntervallEntitet periode) {
        return byggSøktPeriode(periode, null);
    }


    private SøktPeriode<VurdertSøktPeriode.SøktPeriodeData> byggSøktPeriode(DatoIntervallEntitet periode, Arbeidsgiver virksomhet) {
        var dummyObjekt = new VurdertSøktPeriode.SøktPeriodeData() {
            @Override
            public <V> V getPayload() {
                return null;
            }
        };

        if (virksomhet == null) {
            return new SøktPeriode<>(periode, dummyObjekt);
        }
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();

        return new SøktPeriode<>(periode, UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, dummyObjekt);
    }

}
