package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;

import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk.MapAktivitetStatusVedSkjæringstidspunktFraRegelTilVL.mapAktivitetStatusfraRegelmodell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsperiodeTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk.MapOpptjeningAktivitetFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.regelmodell.AktivitetStatusModell;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class MapBGSkjæringstidspunktOgStatuserFraRegelTilVL {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    MapBGSkjæringstidspunktOgStatuserFraRegelTilVL() {
        // for CDI proxy
    }

    @Inject
    public MapBGSkjæringstidspunktOgStatuserFraRegelTilVL(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet mapForSkjæringstidspunktOgStatuser(
        BehandlingReferanse ref,
        AktivitetStatusModell regelModell,
        List<RegelResultat> regelResultater, InntektArbeidYtelseGrunnlag iayGrunnlag) {

        Objects.requireNonNull(regelModell, "regelmodell");
        // Regelresultat brukes kun til logging
        Objects.requireNonNull(regelResultater, "regelresultater");
        if (regelResultater.size() != 2) {
            throw new IllegalStateException("Antall regelresultater må være 2 for å spore regellogg");
        }

        if (regelModell.getAktivitetStatuser().containsAll(Arrays.asList(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus.DP,
            no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus.AAP))) {
            throw new IllegalStateException("Ugyldig kombinasjon av statuser: Kan ikke både ha status AAP og DP samtidig");
        }
        LocalDate skjæringstidspunktForBeregning = regelModell.getSkjæringstidspunktForBeregning();

        LocalDate grunnbeløpDato = ref.getFørsteUttaksdato();

        BigDecimal grunnbeløp = BigDecimal.valueOf(beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, grunnbeløpDato).getVerdi());
        var beregningsgrunnlag = no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(skjæringstidspunktForBeregning)
            .medGrunnbeløp(grunnbeløp)
            // Logging (input -> resultat)
            .medRegelloggSkjæringstidspunkt(regelResultater.get(0).getRegelSporing().getInput(), regelResultater.get(0).getRegelSporing().getSporing())
            .medRegelloggBrukersStatus(regelResultater.get(1).getRegelSporing().getInput(), regelResultater.get(1).getRegelSporing().getSporing())
            .build();
        regelModell.getAktivitetStatuser()
            .forEach(as -> BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(mapAktivitetStatusfraRegelmodell(regelModell, as))
                .build(beregningsgrunnlag));
        var beregningsgrunnlagPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(skjæringstidspunktForBeregning, null)
            .build(beregningsgrunnlag);

        YrkesaktivitetFilter filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(ref.getAktørId()));

        opprettBeregningsgrunnlagPrStatusOgAndelForSkjæringstidspunkt(filter, regelModell, beregningsgrunnlagPeriode);
        return beregningsgrunnlag;
    }

    private void opprettBeregningsgrunnlagPrStatusOgAndelForSkjæringstidspunkt(YrkesaktivitetFilter filter, AktivitetStatusModell regelmodell,
                                                                               BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        var skjæringstidspunkt = regelmodell.getSkjæringstidspunktForBeregning();
        var beregningsperiode = BeregningsperiodeTjeneste.fastsettBeregningsperiodeForATFLAndeler(skjæringstidspunkt);
        regelmodell.getBeregningsgrunnlagPrStatusListe().stream()
            .filter(bgps -> erATFL(bgps.getAktivitetStatus()))
            .forEach(bgps -> bgps.getArbeidsforholdList()
                .forEach(af -> {
                    var arbeidsgiver = MapArbeidsforholdFraRegelTilVL.map(af);
                    var iaRef = InternArbeidsforholdRef.ref(af.getArbeidsforholdId());
                    var ansettelsesPerioder = filter.getYrkesaktiviteterForBeregning().stream()
                        .filter(ya -> ya.gjelderFor(arbeidsgiver, iaRef))
                        .map(ya -> filter.getAnsettelsesPerioder(ya))
                        .flatMap(Collection::stream)
                        .filter(a -> a.getPeriode().getFomDato().isBefore(skjæringstidspunkt))
                        .collect(Collectors.toList());

                    var andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
                        .medArbforholdType(MapOpptjeningAktivitetFraRegelTilVL.map(af.getAktivitet()))
                        .medAktivitetStatus(af.erFrilanser() ? AktivitetStatus.FRILANSER : AktivitetStatus.ARBEIDSTAKER)
                        .medBeregningsperiode(beregningsperiode.getFomDato(), beregningsperiode.getTomDato());
                    LocalDate arbeidsperiodeFom = ansettelsesPerioder.stream().map(a -> a.getPeriode().getFomDato()).min(LocalDate::compareTo).orElse(null);
                    LocalDate arbeidsperiodeTom = ansettelsesPerioder.stream().map(a -> a.getPeriode().getTomDato()).max(LocalDate::compareTo).orElse(null);
                    if (arbeidsperiodeFom != null || af.getReferanseType() != null || af.getArbeidsforholdId() != null) {
                        BGAndelArbeidsforhold.Builder bgArbeidsforholdBuilder = BGAndelArbeidsforhold.builder()
                            .medArbeidsgiver(arbeidsgiver)
                            .medArbeidsforholdRef(af.getArbeidsforholdId())
                            .medArbeidsperiodeTom(arbeidsperiodeTom)
                            .medArbeidsperiodeFom(arbeidsperiodeFom);
                        andelBuilder.medBGAndelArbeidsforhold(bgArbeidsforholdBuilder);
                    }
                    andelBuilder
                        .build(beregningsgrunnlagPeriode);
                }));
        regelmodell.getBeregningsgrunnlagPrStatusListe().stream()
            .filter(bgps -> !(erATFL(bgps.getAktivitetStatus())))
            .forEach(bgps -> BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(mapAktivitetStatusfraRegelmodell(regelmodell, bgps.getAktivitetStatus()))
                .medArbforholdType(MapOpptjeningAktivitetFraRegelTilVL.map(bgps.getAktivitetStatus()))
                .build(beregningsgrunnlagPeriode));
    }

    private boolean erATFL(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus aktivitetStatus) {
        return no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus.ATFL.equals(aktivitetStatus);
    }
}
