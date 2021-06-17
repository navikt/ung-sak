package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.PSBInntektsmeldingerRelevantForBeregning;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

class MapArbeidTest {

    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste = new KompletthetForBeregningTjeneste(null, new TestPSBInntektsmeldingerRelevantForBeregning(), null, null, null, null, null);
    private MapArbeid mapper = new MapArbeid(kompletthetForBeregningTjeneste);

    @Test
    void skal_mappe_arbeid_innenfor_periode_til_vurdering() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().minusDays(3));
        var arbeidsgiverOrgnr = "000000000";
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(periodeTilVurdering, Duration.ZERO)),
            List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
            List.of(),
            List.of(),
            List.of(),
            List.of()));

        var result = mapper.map(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, Set.of(), opprettVilkår(tidlinjeTilVurdering), null);

        assertThat(result).hasSize(1);
        assertThat(result).contains(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, null),
            Map.of(new LukketPeriode(arbeidsperiode.getFomDato(), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)))));
    }

    @Test
    void skal_mappe_arbeid_innenfor_periode_til_vurdering_2() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var periodeDel1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now().minusDays(15));
        var periodeDel2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(14), LocalDate.now());
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().minusDays(3));
        var arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(29), LocalDate.now().minusDays(29));
        var arbeidsgiverOrgnr = "000000000";
        var timerPleieAvBarnetPerDag = Duration.ZERO;
        var timerPleieAvBarnetPerDag1 = Duration.ofHours(4);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(periodeDel1, timerPleieAvBarnetPerDag), new UttakPeriode(periodeDel2, timerPleieAvBarnetPerDag1)),
            List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1)),
                new ArbeidPeriode(arbeidsperiode1, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(7))),
            List.of(),
            List.of(),
            List.of(),
            List.of()));

        var result = mapper.map(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, Set.of(), opprettVilkår(tidlinjeTilVurdering), null);

        assertThat(result).hasSize(1);
        assertThat(result).contains(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, null),
            Map.of(new LukketPeriode(arbeidsperiode.getFomDato(), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)),
                new LukketPeriode(arbeidsperiode1.getFomDato(), arbeidsperiode1.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(7)))));
    }

    @Test
    void skal_mappe_arbeid_innenfor_periode_til_vurdering_prioriter_siste_verdi() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var journalpostId1 = new JournalpostId(2L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD), new KravDokument(journalpostId1, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().minusDays(3));
        var arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(29), LocalDate.now().minusDays(29));
        var arbeidsgiverOrgnr = "000000000";
        var periodeDel1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now().minusDays(20));
        var periodeDel2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeDel1, varighet)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
                List.of(),
                List.of(),
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeDel2, Duration.ZERO)),
                List.of(new ArbeidPeriode(arbeidsperiode1, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(7))),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var result = mapper.map(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, Set.of(), opprettVilkår(tidlinjeTilVurdering), null);

        assertThat(result).hasSize(1);
        assertThat(result).contains(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, null),
            Map.of(new LukketPeriode(arbeidsperiode.getFomDato(), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)),
                new LukketPeriode(arbeidsperiode1.getFomDato(), arbeidsperiode1.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(7)))));
    }

    @Test
    void skal_mappe_arbeid_innenfor_periode_til_vurdering_prioriter_siste_verdi_ved_overlapp() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var journalpostId1 = new JournalpostId(2L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD), new KravDokument(journalpostId1, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(15), LocalDate.now().minusDays(3));
        var arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(29), LocalDate.now().minusDays(9));
        var arbeidsgiverOrgnr = "000000000";
        var periodeDel1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now().minusDays(20));
        var periodeDel2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeDel1, varighet)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
                List.of(),
                List.of(),
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeDel2, Duration.ZERO)),
                List.of(new ArbeidPeriode(arbeidsperiode1, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(7))),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var result = mapper.map(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, Set.of(), opprettVilkår(tidlinjeTilVurdering), null);

        assertThat(result).hasSize(1);
        assertThat(result).contains(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, null),
            Map.of(new LukketPeriode(arbeidsperiode1.getTomDato().plusDays(1), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)),
                new LukketPeriode(arbeidsperiode1.getFomDato(), arbeidsperiode1.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(7)))));
    }

    @Test
    void skal_mappe_arbeid_bare_en_gang_ved_flere_søknader_og_samme_arbeidsforhold() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(20), LocalDate.now());
        var periodeTilVurdering1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now().minusDays(40));
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periodeTilVurdering.toLocalDateInterval(), true),
            new LocalDateSegment<>(periodeTilVurdering1.toLocalDateInterval(), true)));

        var journalpostId = new JournalpostId(1L);
        var journalpostId1 = new JournalpostId(2L);
        var kravDokumenter = Set.of
            (new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD),
                new KravDokument(journalpostId1, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var arbeidsperiode = periodeTilVurdering;
        var arbeidsperiode1 = periodeTilVurdering1;
        var arbeidsgiverOrgnr = "000000000";
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeTilVurdering, varighet)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), null, Duration.ofHours(8), Duration.ofHours(1))),
                List.of(),
                List.of(),
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeTilVurdering1, Duration.ZERO)),
                List.of(new ArbeidPeriode(arbeidsperiode1, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), null, Duration.ofHours(8), Duration.ofHours(7))),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var result = mapper.map(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, Set.of(), opprettVilkår(tidlinjeTilVurdering), null);

        assertThat(result).hasSize(1);
    }

    @Test
    void skal_mappe_arbeid_innenfor_periode_til_vurdering_prioriter_med_arbeidsforholdsId_fra_im() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var journalpostId1 = new JournalpostId(2L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD), new KravDokument(journalpostId1, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().minusDays(3));
        var arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(29), LocalDate.now().minusDays(29));
        var arbeidsgiverOrgnr = "000000000";
        var periodeDel1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now().minusDays(20));
        var periodeDel2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var virksomhet = Arbeidsgiver.virksomhet(arbeidsgiverOrgnr);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeDel1, varighet)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, virksomhet, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
                List.of(),
                List.of(),
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeDel2, Duration.ZERO)),
                List.of(new ArbeidPeriode(arbeidsperiode1, UttakArbeidType.ARBEIDSTAKER, virksomhet, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(7))),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        Set<Inntektsmelding> inntektsmeldinger = Set.of(InntektsmeldingBuilder.builder()
                .medYtelse(FagsakYtelseType.PSB)
                .medArbeidsgiver(virksomhet)
                .medArbeidsforholdId(ref2)
                .medArbeidsforholdId(EksternArbeidsforholdRef.ref("ref"))
                .medJournalpostId("1")
                .medStartDatoPermisjon(periodeTilVurdering.getFomDato())
                .medBeløp(BigDecimal.TEN)
                .medKanalreferanse("AR123")
                .medRefusjon(BigDecimal.TEN)
                .build(),
            InntektsmeldingBuilder.builder()
                .medYtelse(FagsakYtelseType.PSB)
                .medArbeidsgiver(virksomhet)
                .medArbeidsforholdId(ref1)
                .medArbeidsforholdId(EksternArbeidsforholdRef.ref("ref"))
                .medJournalpostId("2")
                .medStartDatoPermisjon(periodeTilVurdering.getFomDato())
                .medBeløp(BigDecimal.TEN)
                .medKanalreferanse("AR124")
                .medRefusjon(BigDecimal.TEN)
                .build());
        var result = mapper.map(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, inntektsmeldinger, opprettVilkår(tidlinjeTilVurdering), null);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, ref1.getUUIDReferanse().toString()),
                Map.of(new LukketPeriode(arbeidsperiode.getFomDato(), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8).dividedBy(inntektsmeldinger.size()), Duration.ofHours(1).dividedBy(inntektsmeldinger.size())),
                    new LukketPeriode(arbeidsperiode1.getFomDato(), arbeidsperiode1.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8).dividedBy(inntektsmeldinger.size()), Duration.ofHours(7).dividedBy(inntektsmeldinger.size())))),
            new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, ref2.getUUIDReferanse().toString()),
                Map.of(new LukketPeriode(arbeidsperiode.getFomDato(), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8).dividedBy(inntektsmeldinger.size()), Duration.ofHours(1).dividedBy(inntektsmeldinger.size())),
                    new LukketPeriode(arbeidsperiode1.getFomDato(), arbeidsperiode1.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8).dividedBy(inntektsmeldinger.size()), Duration.ofHours(7).dividedBy(inntektsmeldinger.size())))));
    }

    private Vilkår opprettVilkår(LocalDateTimeline<Boolean> tidlinjeTilVurdering) {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET);

        tidlinjeTilVurdering.toSegments().forEach(it -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(it.getFom(), it.getTom())));

        return vilkårBuilder.build();
    }

    public static class TestPSBInntektsmeldingerRelevantForBeregning extends PSBInntektsmeldingerRelevantForBeregning {
        @Override
        public Collection<Inntektsmelding> begrensSakInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
            return sakInntektsmeldinger;
        }
    }
}
