package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;

class PerioderMedInaktivitetUtlederTest {

    public static final long DUMMY_BEHANDLING_ID = 1L;
    private PerioderMedInaktivitetUtleder utleder = new PerioderMedInaktivitetUtleder();
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @BeforeEach
    public void setUp() {
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    }

    @Test
    void skal_utlede_tom_tidslinje_hvis_ingen_perioder_er_til_vurdering() {
        var input = new InaktivitetUtlederInput(AktørId.dummy(), LocalDateTimeline.EMPTY_TIMELINE, null);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).isEmpty();
    }

    @Test
    void skal_utlede_tidslinje_hvis_hvor_kun_periodene_hvor_det_ikke_finnes_yrkesaktivitet() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var brukerAktørId = AktørId.dummy();
        iayTjeneste.lagreIayAggregat(DUMMY_BEHANDLING_ID, builder.leggTilAktørArbeid(builder.getAktørArbeidBuilder(brukerAktørId)
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom.minusDays(3)))))));
        var grunnlag = iayTjeneste.hentGrunnlag(DUMMY_BEHANDLING_ID);

        var periodeTilVurdering = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, true)));
        var input = new InaktivitetUtlederInput(brukerAktørId, periodeTilVurdering, grunnlag);
        var utledetTidslinje = utleder.utled(input);

        assertThat(utledetTidslinje).hasSize(1);
        assertThat(utledetTidslinje).containsOnly(new LocalDateSegment<>(tom.minusDays(2), tom, true));
    }
}
