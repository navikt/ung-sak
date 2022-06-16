package no.nav.k9.sak.ytelse.beregning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.ytelse.beregning.adapter.AktivitetStatusMapper;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.RegelBeregnFeriepenger;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.RegelBeregnFeriepengerV2;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

public abstract class BeregnFeriepengerTjeneste {

    private JacksonJsonConfig jacksonJsonConfig = new JacksonJsonConfig();

    protected BeregnFeriepengerTjeneste() {
        //NOSONAR
    }

    public abstract int antallDagerFeriepenger();

    public abstract boolean feriepengeopptjeningForHelg();

    public abstract boolean ubegrensedeDagerVedRefusjon();

    public void beregnFeriepenger(BeregningsresultatEntitet beregningsresultat) {
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat, antallDagerFeriepenger(), feriepengeopptjeningForHelg(), ubegrensedeDagerVedRefusjon());
        String regelInput = toJson(regelModell);

        RegelBeregnFeriepenger regelBeregnFeriepenger = new RegelBeregnFeriepenger();
        Evaluation evaluation = regelBeregnFeriepenger.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        beregningsresultat.setFeriepengerRegelInput(regelInput);
        beregningsresultat.setFeriepengerRegelSporing(sporing);

        mapTilResultatFraRegelModell(beregningsresultat, regelModell);
    }

    public void beregnFeriepengerV2(BeregningsresultatEntitet beregningsresultat) {
        beregnFeriepengerV2(beregningsresultat, LocalDateTimeline.empty());
    }

    public void beregnFeriepengerV2(BeregningsresultatEntitet beregningsresultat, LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> andelerSomKanGiFeriepengerForRelevaneSaker) {
        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat, andelerSomKanGiFeriepengerForRelevaneSaker, antallDagerFeriepenger(), feriepengeopptjeningForHelg(), ubegrensedeDagerVedRefusjon());
        String regelInput = toJson(regelModell);

        RegelBeregnFeriepengerV2 regelBeregnFeriepenger = new RegelBeregnFeriepengerV2();
        Evaluation evaluation = regelBeregnFeriepenger.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        beregningsresultat.setFeriepengerRegelInput(regelInput);
        beregningsresultat.setFeriepengerRegelSporing(sporing);

        mapTilResultatFraRegelModellV2(beregningsresultat, regelModell);
    }

    private String toJson(BeregningsresultatFeriepengerRegelModell grunnlag) {
        var jsonFac = this.jacksonJsonConfig;
        return jsonFac.toJson(grunnlag, BeregnFeriepengerFeil.FACTORY::jsonMappingFeilet);
    }

    static void mapTilResultatFraRegelModell(BeregningsresultatEntitet resultat, BeregningsresultatFeriepengerRegelModell regelModell) {

        if (regelModell.getFeriepengerPeriodeBruker() == null && regelModell.getFeriepengerPeriodeRefusjon() == null) {
            return;
        }

        regelModell.getBeregningsresultatPerioder().forEach(regelBeregningsresultatPeriode -> mapPeriode(resultat, regelBeregningsresultatPeriode));
    }

    static void mapTilResultatFraRegelModellV2(BeregningsresultatEntitet resultat, BeregningsresultatFeriepengerRegelModell regelModell) {
        regelModell.getBeregningsresultatPerioder().forEach(regelBeregningsresultatPeriode -> mapPeriode(resultat, regelBeregningsresultatPeriode));
    }

    private static void mapPeriode(BeregningsresultatEntitet resultat, no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode regelBeregningsresultatPeriode) {
        BeregningsresultatPeriode vlBeregningsresultatPeriode = resultat.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getBeregningsresultatPeriodeFom().equals(regelBeregningsresultatPeriode.getFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke BeregningsresultatPeriode"));
        regelBeregningsresultatPeriode.getBeregningsresultatAndelList().forEach(regelAndel -> mapAndel(vlBeregningsresultatPeriode, regelAndel));
    }

    private static void mapAndel(BeregningsresultatPeriode vlBeregningsresultatPeriode, no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel regelAndel) {
        if (regelAndel.getBeregningsresultatFeriepengerPrÅrListe().isEmpty()) {
            return;
        }
        AktivitetStatus regelAndelAktivitetStatus = AktivitetStatusMapper.fraRegelTilVl(regelAndel);
        String regelArbeidsgiverId = regelAndel.getArbeidsforhold() == null ? null : regelAndel.getArbeidsgiverId();
        String regelArbeidsforholdId = regelAndel.getArbeidsforhold() != null ? regelAndel.getArbeidsforhold().getArbeidsforholdId() : null;
        BeregningsresultatAndel andel = vlBeregningsresultatPeriode.getBeregningsresultatAndelList().stream()
            .filter(vlAndel -> {
                String vlArbeidsforholdRef = vlAndel.getArbeidsforholdRef() == null ? null : vlAndel.getArbeidsforholdRef().getReferanse();
                return Objects.equals(vlAndel.getAktivitetStatus(), regelAndelAktivitetStatus)
                    && Objects.equals(vlAndel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null), regelArbeidsgiverId)
                    && Objects.equals(vlArbeidsforholdRef, regelArbeidsforholdId)
                    && Objects.equals(vlAndel.erBrukerMottaker(), regelAndel.erBrukerMottaker());
            })
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke " + regelAndel));
        regelAndel.getBeregningsresultatFeriepengerPrÅrListe()
            .stream()
            .filter(BeregnFeriepengerTjeneste::erAvrundetÅrsbeløpUlik0)
            .forEach(prÅr -> {
                BigDecimal feriepengerÅrsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP);
                andel.setFeriepengerBeløp(new Beløp(feriepengerÅrsbeløp));
            });
    }

    private static boolean erAvrundetÅrsbeløpUlik0(no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr prÅr) {
        long årsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
        return årsbeløp != 0L;
    }

    interface BeregnFeriepengerFeil extends DeklarerteFeil {
        BeregnFeriepengerFeil FACTORY = FeilFactory.create(BeregnFeriepengerFeil.class); // NOSONAR ok med konstant

        @TekniskFeil(feilkode = "FP-985762", feilmelding = "JSON mapping feilet", logLevel = LogLevel.ERROR)
        Feil jsonMappingFeilet(JsonProcessingException var1);
    }
}
