package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;

class ArbeidBrukerBurdeSøktOmUtlederTest {

    public static final long DUMMY_BEHANDLING_ID = 1L;

    private ArbeidBrukerBurdeSøktOmUtleder utleder = new ArbeidBrukerBurdeSøktOmUtleder();

    private InntektArbeidYtelseTjeneste iayTjeneste;

    @BeforeEach
    public void setUp() {
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    }

    @Test
    void skal_utlede_perioder_hvor_det_burde_vært_søkt_om_ytelse() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.toLocalDateInterval(), true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato().minusDays(10));
        var arbeidsgiverOrgnr = "000000000";
        var arbeidsgiver = Arbeidsgiver.virksomhet(arbeidsgiverOrgnr);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(periodeTilVurdering, Duration.ZERO)),
            List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, timeline, opprettVilkår(timeline), new OpptjeningResultatBuilder(null).build());

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periodeTilVurdering.getFomDato().minusYears(2), periodeTilVurdering.getTomDato().minusDays(3))))
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periodeTilVurdering.getFomDato().minusYears(2), periodeTilVurdering.getTomDato().minusDays(3)))
                    .medProsentsats(BigDecimal.TEN)
                    .medSisteLønnsendringsdato(periodeTilVurdering.getFomDato().minusYears(2))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var manglendeAktivitereFraBruker = utleder.utledFraInput(timeline, timeline, input, grunnlag.getAktørArbeidFraRegister(brukerAktørId));

        assertThat(manglendeAktivitereFraBruker).hasSize(1);
        var identifikator = manglendeAktivitereFraBruker.keySet().iterator().next();
        assertThat(identifikator.getArbeidsgiver()).isEqualTo(arbeidsgiver);
        var manglendeTimeline = manglendeAktivitereFraBruker.get(identifikator);
        assertThat(manglendeTimeline).isNotNull();
        assertThat(manglendeTimeline.stream().anyMatch(it -> it.overlapper(new LocalDateSegment<>(arbeidsperiode.getTomDato(), periodeTilVurdering.getTomDato(), true)))).isTrue();
    }

    @Test
    void skal_IKKE_utlede_perioder_hvor_det_burde_vært_søkt_om_ytelse_hvis_arbeidstid_0_prosent() {
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periodeTilVurdering.toLocalDateInterval(), true)));

        var journalpostId = new JournalpostId(1L);
        var kravDokumenter = Set.of(new KravDokument(journalpostId, LocalDateTime.now(), KravDokumentType.SØKNAD));
        var arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato().minusDays(10));
        var arbeidsgiverOrgnr = "000000000";
        var arbeidsgiver = Arbeidsgiver.virksomhet(arbeidsgiverOrgnr);
        var perioderFraSøknader = Set.of(new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(periodeTilVurdering, Duration.ZERO)),
            List.of(new ArbeidPeriode(arbeidsperiode, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), Duration.ofHours(8), Duration.ofHours(1))),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()));

        var input = new ArbeidstidMappingInput(kravDokumenter, perioderFraSøknader, timeline, opprettVilkår(timeline), new OpptjeningResultatBuilder(null).build());

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periodeTilVurdering.getFomDato().minusYears(2), periodeTilVurdering.getTomDato().minusDays(3))))
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periodeTilVurdering.getFomDato().minusYears(2), periodeTilVurdering.getTomDato().minusDays(3)))
                    .medProsentsats(BigDecimal.ZERO)
                    .medSisteLønnsendringsdato(periodeTilVurdering.getFomDato().minusYears(2))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var manglendeAktivitereFraBruker = utleder.utledFraInput(timeline, timeline, input, grunnlag.getAktørArbeidFraRegister(brukerAktørId));

        assertThat(manglendeAktivitereFraBruker).hasSize(0);
    }

    private Vilkår opprettVilkår(LocalDateTimeline<Boolean> tidlinjeTilVurdering) {
        var vilkårBuilder = new VilkårBuilder(VilkårType.OPPTJENINGSVILKÅRET);

        tidlinjeTilVurdering.toSegments().forEach(it -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(it.getFom(), it.getTom())));

        return vilkårBuilder.build();
    }
}
