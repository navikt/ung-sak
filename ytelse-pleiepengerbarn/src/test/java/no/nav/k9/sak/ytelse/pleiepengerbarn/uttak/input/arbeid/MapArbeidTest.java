package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

class MapArbeidTest {

    public static final long DUMMY_BEHANDLING_ID = 1L;
    private MapArbeid mapper = new MapArbeid();
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @BeforeEach
    public void setUp() {
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    }

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
            List.of(),
            List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        var result = mapper.map(input);

        assertThat(result).hasSize(1);
        assertThat(result).contains(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, null),
            Map.of(new LukketPeriode(arbeidsperiode.getFomDato(), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)))));
    }

    @Test
    void skal_mappe_lage_uttak_for_dagpenger_ved_aktivitet_på_skjæringstidspunktet() {
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
            List.of(),
            List.of()));

        var opptjeningResultat = new OpptjeningResultatBuilder(null);
        var opptjeningStp = periodeTilVurdering.getFomDato().minusDays(1);
        var fomOpptjeningPeriode = opptjeningStp.minusDays(28);
        var opptjening = new Opptjening(fomOpptjeningPeriode, opptjeningStp);
        opptjening.setOpptjeningAktivitet(List.of(new OpptjeningAktivitet(fomOpptjeningPeriode, opptjeningStp, OpptjeningAktivitetType.DAGPENGER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT)));
        opptjeningResultat.leggTil(opptjening);

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), opptjeningResultat.build());
        var result = mapper.map(input);

        assertThat(result).hasSize(2);
        assertThat(result).contains(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, null),
                Map.of(new LukketPeriode(arbeidsperiode.getFomDato(), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)))),
            new Arbeid(new Arbeidsforhold(UttakArbeidType.DAGPENGER.getKode(), null, null, null),
                Map.of(new LukketPeriode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofMinutes((long) (7.5 * 60)), Duration.ZERO))));
    }

    @Test
    void skal_mappe_lage_uttak_for_KUN_YTELSE_på_skjæringstidspunktet() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(periodeTilVurdering, Duration.ZERO)),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()));

        var opptjeningResultat = new OpptjeningResultatBuilder(null);
        var opptjeningStp = periodeTilVurdering.getFomDato().minusDays(1);
        var fomOpptjeningPeriode = opptjeningStp.minusDays(28);
        var opptjening = new Opptjening(fomOpptjeningPeriode, opptjeningStp);
        opptjening.setOpptjeningAktivitet(List.of(new OpptjeningAktivitet(fomOpptjeningPeriode, opptjeningStp.plusDays(10), OpptjeningAktivitetType.FORELDREPENGER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT)));
        opptjeningResultat.leggTil(opptjening);

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), opptjeningResultat.build());
        var result = mapper.map(input);

        assertThat(result).hasSize(1);
        assertThat(result).contains(new Arbeid(new Arbeidsforhold(UttakArbeidType.KUN_YTELSE.getKode(), null, null, null),
            Map.of(new LukketPeriode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofMinutes((long) (7.5 * 60)), Duration.ZERO))));
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
            List.of(),
            List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        var result = mapper.map(input);

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
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeDel2, Duration.ZERO)),
                List.of(new ArbeidPeriode(arbeidsperiode1, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(7))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        var result = mapper.map(input);

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
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeDel2, Duration.ZERO)),
                List.of(new ArbeidPeriode(arbeidsperiode1, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(7))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        var result = mapper.map(input);

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
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeTilVurdering1, Duration.ZERO)),
                List.of(new ArbeidPeriode(arbeidsperiode1, UttakArbeidType.ARBEIDSTAKER, Arbeidsgiver.virksomhet(arbeidsgiverOrgnr), null, Duration.ofHours(8), Duration.ofHours(7))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        var result = mapper.map(input);

        assertThat(result).hasSize(1);
    }

    @Test
    void skal_mappe_arbeid_innenfor_periode_til_vurdering_prioriter_UTEN_arbeidsforholdsId_fra_im() {
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
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeDel2, Duration.ZERO)),
                List.of(new ArbeidPeriode(arbeidsperiode1, UttakArbeidType.ARBEIDSTAKER, virksomhet, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(7))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        var result = mapper.map(input);

        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyInAnyOrder(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, InternArbeidsforholdRef.nullRef().getReferanse()),
                Map.of(new LukketPeriode(arbeidsperiode.getFomDato(), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)),
                    new LukketPeriode(arbeidsperiode1.getFomDato(), arbeidsperiode1.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(7)))));
    }

    @Test
    void skal_fylle_med_kun_ytelse_hvis_ansett_som_inaktiv() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(60), LocalDate.now());
        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), true)));

        var journalpostId = new JournalpostId(1L);
        var journalpostId1 = new JournalpostId(2L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD), new KravDokument(journalpostId1, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(40), LocalDate.now());
        var arbeidsgiverOrgnr = "000000000";
        var periodeDel1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(40), LocalDate.now().minusDays(20));
        var periodeDel2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var virksomhet = Arbeidsgiver.virksomhet(arbeidsgiverOrgnr);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeDel1, varighet)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, virksomhet, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()),
            new PerioderFraSøknad(journalpostId1,
                List.of(new UttakPeriode(periodeDel2, Duration.ZERO)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, virksomhet, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(7))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        input.medInaktivTidslinje(Map.of(new AktivitetIdentifikator(UttakArbeidType.IKKE_YRKESAKTIV, virksomhet, null), new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.toLocalDateInterval(), new WrappedArbeid(new ArbeidPeriode(periodeTilVurdering, UttakArbeidType.IKKE_YRKESAKTIV, virksomhet, null, Duration.ofMinutes((long) (7.5 * 60)), Duration.ofHours(0))))) )));
        var result = mapper.map(input);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, InternArbeidsforholdRef.nullRef().getReferanse()),
                Map.of(new LukketPeriode(arbeidsperiode.getFomDato(), arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(7)))),
            new Arbeid(new Arbeidsforhold(UttakArbeidType.IKKE_YRKESAKTIV.getKode(), arbeidsgiverOrgnr, null, null),
                Map.of(new LukketPeriode(periodeTilVurdering.getFomDato(), arbeidsperiode.getFomDato().minusDays(1)), new ArbeidsforholdPeriodeInfo(Duration.ofMinutes((long) (7.5 * 60)), Duration.ofHours(0)))));
    }

    @Test
    void skal_justere_periode_i_henhold_til_arbeidsforholdet_i_aareg() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        var arbeidsforholdStart = fom.plusDays(3);
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsforholdStart, Tid.TIDENES_ENDE))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        var arbeidsgiverOrgnr = "000000000";
        var periodeDel = arbeidsperiode;
        var virksomhet = Arbeidsgiver.virksomhet(arbeidsgiverOrgnr);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeDel, varighet)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, virksomhet, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        input.medInntektArbeidYtelseGrunnlag(grunnlag)
            .medBruker(brukerAktørId)
            .medSaksnummer(new Saksnummer("asdf"));

        var result = mapper.map(input);
        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyInAnyOrder(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, InternArbeidsforholdRef.nullRef().getReferanse()),
                Map.of(new LukketPeriode(arbeidsforholdStart, arbeidsperiode.getTomDato()), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)))));
    }

    @Test
    void skal_justere_periode_i_henhold_til_arbeidsforholdet_i_aareg_permisjon() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        var arbeidsforholdStart = fom.plusDays(3);
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(yrkesaktivitetBuilder.getPermisjonBuilder()
                    .medPeriode(arbeidsforholdStart, arbeidsforholdStart.plusDays(2))
                    .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                    .medProsentsats(BigDecimal.valueOf(100))
                    .build())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, Tid.TIDENES_ENDE))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        var arbeidsgiverOrgnr = "000000000";
        var periodeDel = arbeidsperiode;
        var virksomhet = Arbeidsgiver.virksomhet(arbeidsgiverOrgnr);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeDel, varighet)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, virksomhet, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        input.medInntektArbeidYtelseGrunnlag(grunnlag)
            .medBruker(brukerAktørId)
            .medSaksnummer(new Saksnummer("asdf"));

        var result = mapper.map(input);
        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyInAnyOrder(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, InternArbeidsforholdRef.nullRef().getReferanse()),
                Map.of(new LukketPeriode(fom, arbeidsforholdStart.minusDays(1)), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)), new LukketPeriode(arbeidsforholdStart.plusDays(3), tom), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)))));
    }

    @Test
    void skal_justere_periode_i_henhold_til_arbeidsforholdet_i_aareg_permisjon_2() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        var arbeidsforholdStart = fom.plusDays(3);
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(yrkesaktivitetBuilder.getPermisjonBuilder()
                    .medPeriode(fom, arbeidsforholdStart)
                    .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                    .medProsentsats(BigDecimal.valueOf(100))
                    .build())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, Tid.TIDENES_ENDE))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        var arbeidsgiverOrgnr = "000000000";
        var periodeDel = arbeidsperiode;
        var virksomhet = Arbeidsgiver.virksomhet(arbeidsgiverOrgnr);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeDel, varighet)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, virksomhet, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        input.medInntektArbeidYtelseGrunnlag(grunnlag)
            .medBruker(brukerAktørId)
            .medSaksnummer(new Saksnummer("asdf"));

        var result = mapper.map(input);
        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyInAnyOrder(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, InternArbeidsforholdRef.nullRef().getReferanse()),
                Map.of(new LukketPeriode(arbeidsforholdStart.plusDays(1), tom), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)))));
    }

    @Test
    void skal_justere_periode_i_henhold_til_dødsfall() {
        var fom = LocalDate.now().minusWeeks(6);
        var tom = LocalDate.now().plusWeeks(6);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        var arbeidsforholdStart = fom.plusDays(3);
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(yrkesaktivitetBuilder.getPermisjonBuilder()
                    .medPeriode(fom, arbeidsforholdStart)
                    .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                    .medProsentsats(BigDecimal.valueOf(100))
                    .build())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, Tid.TIDENES_ENDE))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var tidlinjeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now().minusDays(1), KravDokumentType.SØKNAD));
        var varighet = Duration.ofHours(3);
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        var arbeidsgiverOrgnr = "000000000";
        var periodeDel = arbeidsperiode;
        var virksomhet = Arbeidsgiver.virksomhet(arbeidsgiverOrgnr);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
                List.of(new UttakPeriode(periodeDel, varighet)),
                List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, virksomhet, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, tidlinjeTilVurdering, opprettVilkår(tidlinjeTilVurdering), null);
        var dødsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusWeeks(6));
        input.medInntektArbeidYtelseGrunnlag(grunnlag)
            .medBruker(brukerAktørId)
            .medSaksnummer(new Saksnummer("asdf"))
            .medAutomatiskUtvidelseVedDødsfall(dødsperiode);

        var result = mapper.map(input);
        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyInAnyOrder(new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), arbeidsgiverOrgnr, null, InternArbeidsforholdRef.nullRef().getReferanse()),
                Map.of(new LukketPeriode(arbeidsforholdStart.plusDays(1), dødsperiode.getFomDato().minusDays(1)), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(1)), new LukketPeriode(dødsperiode.getFomDato(), tom), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ZERO))));
    }

    private Vilkår opprettVilkår(LocalDateTimeline<Boolean> tidlinjeTilVurdering) {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET);

        tidlinjeTilVurdering.toSegments().forEach(it -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(it.getFom(), it.getTom())));

        return vilkårBuilder.build();
    }
}
