package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;


import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk.MapAktivitetStatusMedHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk.MapInntektskategoriRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk.MapOpptjeningAktivitetFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk.MapPeriodeÅrsakFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.PeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagPrStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagType;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.SammenligningsGrunnlag;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@Dependent
public class MapBeregningsgrunnlagFraRegelTilVL {
    private static final Map<AktivitetStatus, SammenligningsgrunnlagType> AKTIVITETSTATUS_SAMMENLIGNINGSGRUNNLAGTYPE_MAP = Map.of(
        AktivitetStatus.ATFL_SN, SammenligningsgrunnlagType.SAMMENLIGNING_ATFL_SN,
        AktivitetStatus.AT, SammenligningsgrunnlagType.SAMMENLIGNING_AT,
        AktivitetStatus.FL, SammenligningsgrunnlagType.SAMMENLIGNING_FL,
        AktivitetStatus.SN, SammenligningsgrunnlagType.SAMMENLIGNING_SN
    );

    public MapBeregningsgrunnlagFraRegelTilVL() {
        // for CDI proxy
    }

    private enum Steg {
        FORESLÅ,
        FORDEL,
        FASTSETT,
    }

    public no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet mapForeslåBeregningsgrunnlag(Beregningsgrunnlag resultatGrunnlag, List<RegelResultat> regelResultater, no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet eksisterendeVLGrunnlag) {
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet nyttVLGrunnlag = eksisterendeVLGrunnlag.dypKopi();
        return map(resultatGrunnlag, regelResultater, nyttVLGrunnlag, Steg.FORESLÅ);
    }

    public no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet mapFastsettBeregningsgrunnlag(Beregningsgrunnlag resultatGrunnlag, List<RegelResultat> regelResultater, no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet eksisterendeVLGrunnlag) {
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet nyttVLGrunnlag = eksisterendeVLGrunnlag.dypKopi();
        return map(resultatGrunnlag, regelResultater, nyttVLGrunnlag, Steg.FASTSETT);
    }

    public no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet mapVurdertBeregningsgrunnlag(List<RegelResultat> regelResultater, no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet eksisterendeVLGrunnlag) {
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet nyttVLGrunnlag = eksisterendeVLGrunnlag.dypKopi();
        Iterator<RegelResultat> regelResultatIterator = regelResultater.iterator();
        for (var periode : nyttVLGrunnlag.getBeregningsgrunnlagPerioder()) {
            RegelResultat regelResultat = regelResultatIterator.next();
            BeregningsgrunnlagPeriode.builder(periode)
                .medRegelEvalueringVilkårsvurdering(regelResultat.getRegelSporing().getInput(), regelResultat.getRegelSporing().getSporing())
                .build(nyttVLGrunnlag);
        }
        return nyttVLGrunnlag;
    }

    public no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet mapForFordel(List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode> resultatPerioder, List<RegelResultat> regelResultater,
                                                                                                                       no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet eksisterendeVLGrunnlag) {
        Objects.requireNonNull(resultatPerioder, "resultatPerioder");
        Objects.requireNonNull(regelResultater, "regelResultater");
        if (resultatPerioder.size() != regelResultater.size()) {
            throw new IllegalArgumentException("Antall beregningsresultatperioder ("
                + resultatPerioder.size()
                + ") må være samme som antall regelresultater ("
                + regelResultater.size() + ")");
        }
        mapPerioder(regelResultater, eksisterendeVLGrunnlag, Steg.FORDEL, resultatPerioder);
        return eksisterendeVLGrunnlag;
    }

    private no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet map(Beregningsgrunnlag resultatGrunnlag, List<RegelResultat> regelResultater, no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet eksisterendeVLGrunnlag, Steg steg) {
        mapSammenligningsgrunnlag(resultatGrunnlag.getSammenligningsGrunnlag(), eksisterendeVLGrunnlag);
        if (eksisterendeVLGrunnlag.getSammenligningsgrunnlagPrStatusListe().isEmpty()) {
            mapSammenligningsgrunnlagPrStatus(resultatGrunnlag.getSammenligningsGrunnlagPrAktivitetstatus(), eksisterendeVLGrunnlag);
        }

        Objects.requireNonNull(resultatGrunnlag, "resultatGrunnlag");
        Objects.requireNonNull(regelResultater, "regelResultater");
        if (resultatGrunnlag.getBeregningsgrunnlagPerioder().size() != regelResultater.size()) {
            throw new IllegalArgumentException("Antall beregningsresultatperioder ("
                + resultatGrunnlag.getBeregningsgrunnlagPerioder().size()
                + ") må være samme som antall regelresultater ("
                + regelResultater.size() + ")");
        }
        MapAktivitetStatusMedHjemmel.mapAktivitetStatusMedHjemmel(resultatGrunnlag.getAktivitetStatuser(), eksisterendeVLGrunnlag, resultatGrunnlag.getBeregningsgrunnlagPerioder().get(0));

        mapPerioder(regelResultater, eksisterendeVLGrunnlag, steg, resultatGrunnlag.getBeregningsgrunnlagPerioder());

        return eksisterendeVLGrunnlag;
    }

    private void mapPerioder(List<RegelResultat> regelResultater, BeregningsgrunnlagEntitet eksisterendeVLGrunnlag, Steg steg, List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder) {
        Iterator<RegelResultat> resultat = regelResultater.iterator();

        int vlBGnummer = 0;
        for (var resultatBGPeriode : beregningsgrunnlagPerioder) {

            RegelResultat regelResultat = resultat.next();
            BeregningsgrunnlagPeriode eksisterendePeriode = (vlBGnummer < eksisterendeVLGrunnlag.getBeregningsgrunnlagPerioder().size())
                ? eksisterendeVLGrunnlag.getBeregningsgrunnlagPerioder().get(vlBGnummer)
                : null;
            BeregningsgrunnlagPeriode mappetPeriode = mapBeregningsgrunnlagPeriode(resultatBGPeriode, regelResultat, eksisterendePeriode, eksisterendeVLGrunnlag, steg);
            for (BeregningsgrunnlagPrStatus regelAndel : resultatBGPeriode.getBeregningsgrunnlagPrStatus()) {
                if (regelAndel.getAndelNr() == null) {
                    mapAndelMedArbeidsforhold(mappetPeriode, regelAndel);
                } else {
                    mapAndel(mappetPeriode, regelAndel, steg);
                }
            }
            vlBGnummer++;
            fastsettAgreggerteVerdier(mappetPeriode, eksisterendeVLGrunnlag);
        }
    }

    private void mapAndel(BeregningsgrunnlagPeriode mappetPeriode, BeregningsgrunnlagPrStatus regelAndel, Steg steg) {
        mappetPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(bgpsa -> regelAndel.getAndelNr().equals(bgpsa.getAndelsnr()))
            .forEach(resultatAndel -> mapBeregningsgrunnlagPrStatus(mappetPeriode, regelAndel, resultatAndel, steg));
    }

    private void mapAndelMedArbeidsforhold(BeregningsgrunnlagPeriode mappetPeriode, BeregningsgrunnlagPrStatus regelAndel) {
        for (BeregningsgrunnlagPrArbeidsforhold regelAndelForArbeidsforhold : regelAndel.getArbeidsforhold()) {
            Optional<BeregningsgrunnlagPrStatusOgAndel> andelOpt = mappetPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(bgpsa -> regelAndelForArbeidsforhold.getAndelNr().equals(bgpsa.getAndelsnr()))
                .findFirst();
            if (andelOpt.isPresent()) {
                BeregningsgrunnlagPrStatusOgAndel resultatAndel = andelOpt.get();
                mapBeregningsgrunnlagPrStatusForATKombinert(mappetPeriode, regelAndel, resultatAndel);
            }
        }
    }

    private void fastsettAgreggerteVerdier(BeregningsgrunnlagPeriode periode, no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet eksisterendeVLGrunnlag) {
        Optional<BigDecimal> bruttoPrÅr = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(bgpsa -> bgpsa.getBruttoPrÅr() != null)
            .map(BeregningsgrunnlagPrStatusOgAndel::getBruttoPrÅr)
            .reduce(BigDecimal::add);
        Optional<BigDecimal> avkortetPrÅr = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(bgpsa -> bgpsa.getAvkortetPrÅr() != null)
            .map(BeregningsgrunnlagPrStatusOgAndel::getAvkortetPrÅr)
            .reduce(BigDecimal::add);
        Optional<BigDecimal> redusertPrÅr = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(bgpsa -> bgpsa.getRedusertPrÅr() != null)
            .map(BeregningsgrunnlagPrStatusOgAndel::getRedusertPrÅr)
            .reduce(BigDecimal::add);
        BeregningsgrunnlagPeriode.builder(periode)
            .medBruttoPrÅr(bruttoPrÅr.orElse(null))
            .medAvkortetPrÅr(avkortetPrÅr.orElse(null))
            .medRedusertPrÅr(redusertPrÅr.orElse(null))
            .build(eksisterendeVLGrunnlag);
    }

    private void mapBeregningsgrunnlagPrStatusForATKombinert(BeregningsgrunnlagPeriode vlBGPeriode,
                                                             BeregningsgrunnlagPrStatus resultatBGPStatus,
                                                             BeregningsgrunnlagPrStatusOgAndel vlBGPAndel) {
        for (BeregningsgrunnlagPrArbeidsforhold arbeidsforhold : resultatBGPStatus.getArbeidsforhold()) {
            if (gjelderSammeAndel(vlBGPAndel, arbeidsforhold)) {
                BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = settFasteVerdier(vlBGPAndel, arbeidsforhold);
                if (skalByggeBGArbeidsforhold(arbeidsforhold, vlBGPAndel)) {
                    BGAndelArbeidsforhold.Builder bgAndelArbeidsforhold = mapArbeidsforhold(vlBGPAndel, arbeidsforhold);
                    andelBuilder.medBGAndelArbeidsforhold(bgAndelArbeidsforhold);
                }
                andelBuilder
                    .build(vlBGPeriode);
                return;
            }
        }
    }

    private BGAndelArbeidsforhold.Builder mapArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel vlBGPAndel,
                                                            BeregningsgrunnlagPrArbeidsforhold arbeidsforhold) {
        return BGAndelArbeidsforhold
            .builder(vlBGPAndel.getBgAndelArbeidsforhold().orElse(null))
            .medNaturalytelseBortfaltPrÅr(arbeidsforhold.getNaturalytelseBortfaltPrÅr().orElse(null))
            .medNaturalytelseTilkommetPrÅr(arbeidsforhold.getNaturalytelseTilkommetPrÅr().orElse(null))
            .medRefusjonskravPrÅr(arbeidsforhold.getRefusjonskravPrÅr().orElse(null));
    }

    private BeregningsgrunnlagPrStatusOgAndel.Builder settFasteVerdier(BeregningsgrunnlagPrStatusOgAndel vlBGPAndel, BeregningsgrunnlagPrArbeidsforhold arbeidsforhold) {
        return BeregningsgrunnlagPrStatusOgAndel.builder(vlBGPAndel)
            .medBeregnetPrÅr(verifisertBeløp(arbeidsforhold.getBeregnetPrÅr()))
            .medOverstyrtPrÅr(verifisertBeløp(arbeidsforhold.getOverstyrtPrÅr()))
            .medFordeltPrÅr(verifisertBeløp(arbeidsforhold.getFordeltPrÅr()))
            .medAvkortetPrÅr(verifisertBeløp(arbeidsforhold.getAvkortetPrÅr()))
            .medRedusertPrÅr(verifisertBeløp(arbeidsforhold.getRedusertPrÅr()))
            .medMaksimalRefusjonPrÅr(arbeidsforhold.getMaksimalRefusjonPrÅr())
            .medAvkortetRefusjonPrÅr(arbeidsforhold.getAvkortetRefusjonPrÅr())
            .medRedusertRefusjonPrÅr(arbeidsforhold.getRedusertRefusjonPrÅr())
            .medAvkortetBrukersAndelPrÅr(verifisertBeløp(arbeidsforhold.getAvkortetBrukersAndelPrÅr()))
            .medRedusertBrukersAndelPrÅr(verifisertBeløp(arbeidsforhold.getRedusertBrukersAndelPrÅr()))
            .medBeregningsperiode(
                arbeidsforhold.getBeregningsperiode() == null ? null : arbeidsforhold.getBeregningsperiode().getFomOrNull(),
                arbeidsforhold.getBeregningsperiode() == null ? null : arbeidsforhold.getBeregningsperiode().getTomOrNull()
            )
            .medFastsattAvSaksbehandler(arbeidsforhold.getFastsattAvSaksbehandler())
            .medLagtTilAvSaksbehandler(arbeidsforhold.getLagtTilAvSaksbehandler())
            .medArbforholdType(MapOpptjeningAktivitetFraRegelTilVL.map(arbeidsforhold.getArbeidsforhold().getAktivitet()))
            .medInntektskategori(MapInntektskategoriRegelTilVL.map(arbeidsforhold.getInntektskategori()));
    }

    private boolean skalByggeBGArbeidsforhold(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, BeregningsgrunnlagPrStatusOgAndel vlBGPAndel) {
        return vlBGPAndel.getBgAndelArbeidsforhold().isPresent() &&
            (arbeidsforhold.getNaturalytelseBortfaltPrÅr().isPresent()
            || arbeidsforhold.getNaturalytelseTilkommetPrÅr().isPresent());
    }

    private BigDecimal verifisertBeløp(BigDecimal beløp) {
        return beløp == null ? null : beløp.max(BigDecimal.ZERO);
    }

    private boolean gjelderSammeAndel(BeregningsgrunnlagPrStatusOgAndel vlBGPAndel, BeregningsgrunnlagPrArbeidsforhold arbeidsforhold) {
        if (vlBGPAndel.getAktivitetStatus().erFrilanser()) {
            return arbeidsforhold.erFrilanser();
        }
        if (arbeidsforhold.erFrilanser()) {
            return false;
        }
        if (!vlBGPAndel.getInntektskategori().equals(MapInntektskategoriRegelTilVL.map(arbeidsforhold.getInntektskategori()))) {
            return false;
        }
        if (!matcherArbeidsgivere(vlBGPAndel, arbeidsforhold)) {
            return false;
        }
        if (!matcherOpptjeningsaktivitet(vlBGPAndel, arbeidsforhold)) {
            return false;
        }
        return vlBGPAndel.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getArbeidsforholdRef)
            .filter(InternArbeidsforholdRef::gjelderForSpesifiktArbeidsforhold)
            .map(ref -> Objects.equals(ref, InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforhold().getArbeidsforholdId())))
            .orElse(arbeidsforhold.getArbeidsforhold().getArbeidsforholdId() == null);
    }

    private boolean matcherOpptjeningsaktivitet(BeregningsgrunnlagPrStatusOgAndel vlBGPAndel, BeregningsgrunnlagPrArbeidsforhold arbeidsforhold) {
        if (arbeidsforhold.getArbeidsforhold() != null) {
            return Objects.equals(vlBGPAndel.getArbeidsforholdType(), MapOpptjeningAktivitetFraRegelTilVL.map(arbeidsforhold.getArbeidsforhold().getAktivitet()));
        }
        return vlBGPAndel.getArbeidsforholdType() == null;
    }

    private boolean matcherArbeidsgivere(BeregningsgrunnlagPrStatusOgAndel andel, BeregningsgrunnlagPrArbeidsforhold forhold) {
        Arbeidsgiver arbeidsgiver = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver).orElse(null);
        if (forhold.getArbeidsgiverId() == null) {
            return arbeidsgiver == null;
        } else
            return arbeidsgiver != null && Objects.equals(forhold.getArbeidsgiverId(), arbeidsgiver.getIdentifikator());
    }

    private void mapBeregningsgrunnlagPrStatus(BeregningsgrunnlagPeriode vlBGPeriode,
                                               BeregningsgrunnlagPrStatus resultatBGPStatus,
                                               BeregningsgrunnlagPrStatusOgAndel vlBGPStatusOgAndel,
                                               Steg steg) {
        boolean gjelderForeslå = steg.equals(Steg.FORESLÅ);
        BeregningsgrunnlagPrStatusOgAndel.builder(vlBGPStatusOgAndel)
            .medBeregnetPrÅr(verifisertBeløp(resultatBGPStatus.getBeregnetPrÅr()))
            .medOverstyrtPrÅr(verifisertBeløp(resultatBGPStatus.getOverstyrtPrÅr()))
            .medFordeltPrÅr(verifisertBeløp(resultatBGPStatus.getFordeltPrÅr()))
            .medAvkortetPrÅr(verifisertBeløp(resultatBGPStatus.getAvkortetPrÅr()))
            .medRedusertPrÅr(verifisertBeløp(resultatBGPStatus.getRedusertPrÅr()))
            .medAvkortetBrukersAndelPrÅr(gjelderForeslå ? null : verifisertBeløp(resultatBGPStatus.getAvkortetPrÅr()))
            .medRedusertBrukersAndelPrÅr(gjelderForeslå ? null : verifisertBeløp(resultatBGPStatus.getRedusertPrÅr()))
            .medMaksimalRefusjonPrÅr(gjelderForeslå ? null : BigDecimal.ZERO)
            .medAvkortetRefusjonPrÅr(gjelderForeslå ? null : BigDecimal.ZERO)
            .medRedusertRefusjonPrÅr(gjelderForeslå ? null : BigDecimal.ZERO)
            .medBeregningsperiode(
                resultatBGPStatus.getBeregningsperiode() == null ? null : resultatBGPStatus.getBeregningsperiode().getFomOrNull(),
                resultatBGPStatus.getBeregningsperiode() == null ? null : resultatBGPStatus.getBeregningsperiode().getTomOrNull()
            )
            .medPgi(resultatBGPStatus.getGjennomsnittligPGI(), resultatBGPStatus.getPgiListe())
            .medÅrsbeløpFraTilstøtendeYtelse(resultatBGPStatus.getÅrsbeløpFraTilstøtendeYtelse())
            .medNyIArbeidslivet(resultatBGPStatus.getNyIArbeidslivet())
            .medInntektskategori(MapInntektskategoriRegelTilVL.map(resultatBGPStatus.getInntektskategori()))
            .medFastsattAvSaksbehandler(resultatBGPStatus.erFastsattAvSaksbehandler())
            .medLagtTilAvSaksbehandler(resultatBGPStatus.erLagtTilAvSaksbehandler())
            .medBesteberegningPrÅr(resultatBGPStatus.getBesteberegningPrÅr())
            .medOrginalDagsatsFraTilstøtendeYtelse(resultatBGPStatus.getOrginalDagsatsFraTilstøtendeYtelse())
            .build(vlBGPeriode);
    }

    private BeregningsgrunnlagPeriode mapBeregningsgrunnlagPeriode(final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode resultatGrunnlagPeriode,
                                                                   RegelResultat regelResultat,
                                                                   final BeregningsgrunnlagPeriode vlBGPeriode,
                                                                   no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet eksisterendeVLGrunnlag,
                                                                   Steg steg) {
        if (vlBGPeriode == null) {
            BeregningsgrunnlagPeriode.Builder builder = BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(
                    resultatGrunnlagPeriode.getBeregningsgrunnlagPeriode().getFom(),
                    resultatGrunnlagPeriode.getBeregningsgrunnlagPeriode().getTomOrNull()
                )
                .leggTilPeriodeÅrsaker(mapPeriodeÅrsaker(resultatGrunnlagPeriode.getPeriodeÅrsaker()));
            leggTilRegelEvaluering(regelResultat, steg, builder);
            BeregningsgrunnlagPeriode periode = builder
                .build(eksisterendeVLGrunnlag);
            opprettBeregningsgrunnlagPrStatusOgAndel(eksisterendeVLGrunnlag.getBeregningsgrunnlagPerioder().get(0), periode);
            return periode;
        }
        BeregningsgrunnlagPeriode.Builder periodeBuilder = BeregningsgrunnlagPeriode.builder(vlBGPeriode)
            .medBeregningsgrunnlagPeriode(
                resultatGrunnlagPeriode.getBeregningsgrunnlagPeriode().getFom(),
                resultatGrunnlagPeriode.getBeregningsgrunnlagPeriode().getTomOrNull()
            )
            .tilbakestillPeriodeÅrsaker()
            .leggTilPeriodeÅrsaker(mapPeriodeÅrsaker(resultatGrunnlagPeriode.getPeriodeÅrsaker()));
        BeregningsgrunnlagPeriode.Builder periodeBuilderMedRegel = leggTilRegelEvaluering(regelResultat, steg, periodeBuilder);
        regelResultat.getRegelSporingFinnGrenseverdi()
            .ifPresent(regelSporing -> periodeBuilderMedRegel.medRegelEvalueringFinnGrenseverdi(regelSporing.getInput(), regelSporing.getSporing()));
        periodeBuilder.build(eksisterendeVLGrunnlag);
        return vlBGPeriode;
    }

    private BeregningsgrunnlagPeriode.Builder leggTilRegelEvaluering(RegelResultat regelResultat, Steg steg, BeregningsgrunnlagPeriode.Builder periodeBuilder) {
        if (steg.equals(Steg.FORESLÅ)) {
            periodeBuilder.medRegelEvalueringForeslå(regelResultat.getRegelSporing().getInput(), regelResultat.getRegelSporing().getSporing());
        } else if (steg.equals(Steg.FORDEL)) {
            periodeBuilder.medRegelEvalueringFordel(regelResultat.getRegelSporing().getInput(), regelResultat.getRegelSporing().getSporing());
        } else {
            periodeBuilder.medRegelEvalueringFastsett(regelResultat.getRegelSporing().getInput(), regelResultat.getRegelSporing().getSporing());
        }
        return periodeBuilder;
    }

    private void opprettBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPeriode kopierFra, BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        kopierFra.getBeregningsgrunnlagPrStatusOgAndelList().forEach(bgpsa -> {
            BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
                .medArbforholdType(bgpsa.getArbeidsforholdType())
                .medAktivitetStatus(bgpsa.getAktivitetStatus())
                .medInntektskategori(bgpsa.getInntektskategori());
            Optional<Arbeidsgiver> arbeidsgiver = bgpsa.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver);
            Optional<InternArbeidsforholdRef> arbeidsforholdRef = bgpsa.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef);
            if (arbeidsgiver.isPresent() || arbeidsforholdRef.isPresent()) {
                BGAndelArbeidsforhold arbeidsforhold = bgpsa.getBgAndelArbeidsforhold().get();
                BGAndelArbeidsforhold.Builder bgAndelArbeidsforhold = BGAndelArbeidsforhold.builder()
                    .medArbeidsgiver(arbeidsgiver.orElse(null))
                    .medArbeidsforholdRef(arbeidsforholdRef.orElse(null))
                    .medArbeidsperiodeFom(arbeidsforhold.getArbeidsperiodeFom())
                    .medArbeidsperiodeTom(arbeidsforhold.getArbeidsperiodeTom().orElse(null));
                andelBuilder
                    .medBGAndelArbeidsforhold(bgAndelArbeidsforhold);
            }
            andelBuilder.build(beregningsgrunnlagPeriode);
        });
    }

    private void mapSammenligningsgrunnlag(final SammenligningsGrunnlag resultatSammenligningsGrunnlag, no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet eksisterendeVLGrunnlag) {
        if (resultatSammenligningsGrunnlag != null) {
            Sammenligningsgrunnlag.builder()
                .medSammenligningsperiode(
                    resultatSammenligningsGrunnlag.getSammenligningsperiode().getFom(),
                    resultatSammenligningsGrunnlag.getSammenligningsperiode().getTom()
                )
                .medRapportertPrÅr(resultatSammenligningsGrunnlag.getRapportertPrÅr())
                .medAvvikPromille(resultatSammenligningsGrunnlag.getAvvikPromille())
                .build(eksisterendeVLGrunnlag);
        }
    }

    private void mapSammenligningsgrunnlagPrStatus(final EnumMap<AktivitetStatus, SammenligningsGrunnlag> sammenligningsgrunnlag,
                                                   no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet eksisterendeVLGrunnlag) {
        sammenligningsgrunnlag.entrySet().forEach(s -> {
            if(!AKTIVITETSTATUS_SAMMENLIGNINGSGRUNNLAGTYPE_MAP.containsKey(s.getKey())) {
                throw new IllegalArgumentException("Finner ingen mapping mellom AktivitetStatus "+s.getKey()+" og SammenligningsgrunnlagType");
            }
            BeregningsgrunnlagEntitet.builder(eksisterendeVLGrunnlag).leggTilSammenligningsgrunnlag(SammenligningsgrunnlagPrStatus.builder()
                .medSammenligningsperiode(s.getValue().getSammenligningsperiode().getFom(), s.getValue().getSammenligningsperiode().getTom())
                .medRapportertPrÅr(s.getValue().getRapportertPrÅr())
                .medAvvikPromille(s.getValue().getAvvikPromille())
                .medSammenligningsgrunnlagType(AKTIVITETSTATUS_SAMMENLIGNINGSGRUNNLAGTYPE_MAP.get(s.getKey())));
        });
    }

    private List<PeriodeÅrsak> mapPeriodeÅrsaker(List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodeÅrsak> periodeÅrsaker) {
        return periodeÅrsaker.stream()
            .map(MapPeriodeÅrsakFraRegelTilVL::map)
            .collect(Collectors.toList());
    }
}
