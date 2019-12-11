package no.nav.folketrygdloven.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.PeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

final class BeregningsgrunnlagVerifiserer {

    private static final Logger LOG = LoggerFactory.getLogger(BeregningsgrunnlagVerifiserer.class);

    private BeregningsgrunnlagVerifiserer() {}

    static void verifiserOppdatertBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        Objects.requireNonNull(beregningsgrunnlag.getSkjæringstidspunkt(), "Skjæringstidspunkt");
        verifiserIkkeTomListe(beregningsgrunnlag.getBeregningsgrunnlagPerioder(), "BeregningsgrunnlagPerioder");
        verifiserIkkeTomListe(beregningsgrunnlag.getAktivitetStatuser(), "Aktivitetstatuser");
        verfiserBeregningsgrunnlagPerioder(beregningsgrunnlag);
        verfiserBeregningsgrunnlagAndeler(beregningsgrunnlag, BeregningsgrunnlagVerifiserer::verifiserOpprettetAndel);
    }

    private static void verfiserBeregningsgrunnlagPerioder(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder();
        for (int i = 0; i < beregningsgrunnlagPerioder.size(); i++) {
            BeregningsgrunnlagPeriode periode = beregningsgrunnlagPerioder.get(i);
            Objects.requireNonNull(periode.getBeregningsgrunnlagPeriodeFom(), "BeregningsgrunnlagperiodeFom");
            verifiserIkkeTomListe(periode.getBeregningsgrunnlagPrStatusOgAndelList(), "BeregningsgrunnlagPrStatusOgAndelList");
            if (i > 0) {
                verifiserIkkeTomListe(periode.getPeriodeÅrsaker(), "PeriodeÅrsaker");
            }
        }
    }

    private static void verfiserBeregningsgrunnlagAndeler(BeregningsgrunnlagEntitet beregningsgrunnlag, Consumer<BeregningsgrunnlagPrStatusOgAndel> verifiserAndel) {
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(bgp -> bgp.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .forEach(verifiserAndel);
    }

    private static void verifiserOpprettetAndel(BeregningsgrunnlagPrStatusOgAndel andel) {
        Objects.requireNonNull(andel.getAktivitetStatus(), "Aktivitetstatus");
        Objects.requireNonNull(andel.getAndelsnr(), "Andelsnummer");
        Objects.requireNonNull(andel.getArbeidsforholdType(), "ArbeidsforholdType");
        if (andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)) {
            if (andel.getArbeidsforholdType().equals(OpptjeningAktivitetType.ARBEID)) {
                verifiserOptionalPresent(andel.getBgAndelArbeidsforhold(), "BgAndelArbeidsforhold");
            }
            Objects.requireNonNull(andel.getBeregningsperiodeFom(), "BeregningsperiodeFom");
            Objects.requireNonNull(andel.getBeregningsperiodeTom(), "BeregningsperiodeTom");
            if (andel.getBgAndelArbeidsforhold().isPresent()) {
                Objects.requireNonNull(andel.getBgAndelArbeidsforhold().get().getArbeidsperiodeFom());
                Objects.requireNonNull(andel.getBgAndelArbeidsforhold().get().getArbeidsperiodeTom());
            }
        }
    }

    static void verifiserForeslåttBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        verifiserOppdatertBeregningsgrunnlag(beregningsgrunnlag);
        verfiserBeregningsgrunnlagAndeler(beregningsgrunnlag, BeregningsgrunnlagVerifiserer::verifiserForeslåttAndel);
        Sammenligningsgrunnlag sg = beregningsgrunnlag.getSammenligningsgrunnlag();
        if (sg != null) {
            Objects.requireNonNull(sg.getRapportertPrÅr(), "RapportertPrÅr");
            Objects.requireNonNull(sg.getAvvikPromille(), "AvvikPromille");
            Objects.requireNonNull(sg.getSammenligningsperiodeFom(), "SammenligningsperiodeFom");
            Objects.requireNonNull(sg.getSammenligningsperiodeTom(), "SammenligningsperiodeTom");
        }
    }

    private static void verifiserFordeltBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        verifiserOppdatertBeregningsgrunnlag(beregningsgrunnlag);
        verfiserBeregningsgrunnlagAndeler(beregningsgrunnlag, BeregningsgrunnlagVerifiserer::verifiserFordeltAndel);
        Sammenligningsgrunnlag sg = beregningsgrunnlag.getSammenligningsgrunnlag();
        if (sg != null) {
            Objects.requireNonNull(sg.getRapportertPrÅr(), "RapportertPrÅr");
            Objects.requireNonNull(sg.getAvvikPromille(), "AvvikPromille");
            Objects.requireNonNull(sg.getSammenligningsperiodeFom(), "SammenligningsperiodeFom");
            Objects.requireNonNull(sg.getSammenligningsperiodeTom(), "SammenligningsperiodeTom");
        }
    }

    static void verifiserFastsattBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag, AktivitetGradering aktivitetGradering) {
        verifiserFordeltBeregningsgrunnlag(beregningsgrunnlag);
        verfiserBeregningsgrunnlagAndeler(beregningsgrunnlag, BeregningsgrunnlagVerifiserer::verifiserFastsattAndel);
        if (aktivitetGradering != null) {
            verifiserAtAndelerSomGraderesHarGrunnlag(beregningsgrunnlag, aktivitetGradering);
        }
    }

    private static void verifiserAtAndelerSomGraderesHarGrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag, AktivitetGradering aktivitetGradering) {
        List<BeregningsgrunnlagPrStatusOgAndel> andelerMedGraderingUtenBG = GraderingUtenBeregningsgrunnlagTjeneste.finnAndelerMedGraderingUtenBG(beregningsgrunnlag, aktivitetGradering);
        if (!andelerMedGraderingUtenBG.isEmpty()) {
            LOG.info("Gradering på bgandeler {} med disse graderingsaktivitetene: {}", andelerMedGraderingUtenBG, aktivitetGradering);
            throw BeregningsgrunnlagVerifisererFeil.FEILFACTORY.graderingUtenBG(andelerMedGraderingUtenBG.get(0).getAktivitetStatus().getKode()).toException();
        }
    }

    private static void verifiserForeslåttAndel(BeregningsgrunnlagPrStatusOgAndel andel) {
        Objects.requireNonNull(andel.getInntektskategori(), "Inntektskategori");
        if (andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            if (!periodeOppståttGrunnetGradering(andel.getBeregningsgrunnlagPeriode().getPeriodeÅrsaker())) {
                Objects.requireNonNull(andel.getPgiSnitt(), "PgiSnitt");
                Objects.requireNonNull(andel.getPgi1(), "PgiÅr1");
                Objects.requireNonNull(andel.getPgi2(), "PgiÅr2");
                Objects.requireNonNull(andel.getPgi3(), "PgiÅr3");
            }
        } else if (!andel.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE)) {
            LocalDate bgPeriodeFom = andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlagPeriodeFom();
            String andelBeskrivelse = "andel" + andel.getAktivitetStatus() + " i perioden fom " + bgPeriodeFom;
            Objects.requireNonNull(andel.getBruttoPrÅr(), "BruttoPrÅr er null for " + andelBeskrivelse);
            Objects.requireNonNull(andel.getBeregnetPrÅr(), "beregnetPrÅr er null for " + andelBeskrivelse);
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER) || andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)) {
            Objects.requireNonNull(andel.getÅrsbeløpFraTilstøtendeYtelse(), "ÅrsbeløpFraTilstøtendeYtelse");
            Objects.requireNonNull(andel.getOrginalDagsatsFraTilstøtendeYtelse(), "originalDagsatsFraTilstøtendeYtelse");
        }
    }

    private static boolean periodeOppståttGrunnetGradering(List<PeriodeÅrsak> periodeÅrsaker) {
        return periodeÅrsaker.stream().anyMatch(p -> p.equals(PeriodeÅrsak.GRADERING));
    }

    private static void verifiserFordeltAndel(BeregningsgrunnlagPrStatusOgAndel andel) {
        Objects.requireNonNull(andel.getInntektskategori(), "Inntektskategori");
        if (!andel.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE) && !andel.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE)) {
            Objects.requireNonNull(andel.getBruttoPrÅr(), "BruttoPrÅr");
            if (andel.getBeregnetPrÅr() == null) {
                Objects.requireNonNull(andel.getFordeltPrÅr(), "fordeltPrÅr");
            }
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER) || andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)) {
            Objects.requireNonNull(andel.getÅrsbeløpFraTilstøtendeYtelse(), "ÅrsbeløpFraTilstøtendeYtelse");
            Objects.requireNonNull(andel.getOrginalDagsatsFraTilstøtendeYtelse(), "originalDagsatsFraTilstøtendeYtelse");
        }
    }


    private static void verifiserFastsattAndel(BeregningsgrunnlagPrStatusOgAndel andel) {
        Objects.requireNonNull(andel.getBruttoPrÅr(), "BruttoPrÅr");
        Objects.requireNonNull(andel.getAvkortetPrÅr(), "AvkortetPrÅr");
        Objects.requireNonNull(andel.getRedusertPrÅr(), "RedusertPrÅr");
        Objects.requireNonNull(andel.getAvkortetBrukersAndelPrÅr(), "AvkortetBrukersAndelPrÅr");
        Objects.requireNonNull(andel.getRedusertBrukersAndelPrÅr(), "RedusertBrukersAndelPrÅr");
        Objects.requireNonNull(andel.getDagsatsBruker(), "DagsatsBruker");
        Objects.requireNonNull(andel.getMaksimalRefusjonPrÅr(), "MaksimalRefusjonPrÅr");
        Objects.requireNonNull(andel.getAvkortetRefusjonPrÅr(), "AvkortetRefusjonPrÅr");
        Objects.requireNonNull(andel.getRedusertRefusjonPrÅr(), "RedusertRefusjonPrÅr");
        Objects.requireNonNull(andel.getDagsatsArbeidsgiver(), "DagsatsArbeidsgiver");
    }


    private static void verifiserIkkeTomListe(Collection<?> liste, String obj) {
        Objects.requireNonNull(liste, "Liste");
        if (liste.isEmpty()) {
            throw BeregningsgrunnlagVerifisererFeil.FEILFACTORY.verifiserIkkeTomListeFeil(obj).toException();
        }
    }

    private static void verifiserOptionalPresent(Optional<?> opt, String obj) {
        Objects.requireNonNull(opt, "Optional");
        if (!opt.isPresent()) {
            throw BeregningsgrunnlagVerifisererFeil.FEILFACTORY.verifiserOptionalPresentFeil(obj).toException();
        }
    }

    private interface BeregningsgrunnlagVerifisererFeil extends DeklarerteFeil {
        BeregningsgrunnlagVerifisererFeil FEILFACTORY = FeilFactory.create(BeregningsgrunnlagVerifisererFeil.class);

        @TekniskFeil(feilkode = "FP-370742", feilmelding = "Postcondition feilet: Beregningsgrunnlag i ugyldig tilstand etter steg. Listen %s er tom, men skulle ikke vært det.", logLevel = LogLevel.ERROR)
        Feil verifiserIkkeTomListeFeil(String obj);

        @TekniskFeil(feilkode = "FP-370743", feilmelding = "Postcondition feilet: Beregningsgrunnlag i ugyldig tilstand etter steg. Optional %s er ikke present, men skulle ha vært det.", logLevel = LogLevel.ERROR)
        Feil verifiserOptionalPresentFeil(String obj);

        @TekniskFeil(feilkode = "FP-370746", feilmelding = "Det mangler beregningsgrunnlag på en andel som skal graderes, ugyldig tilstand. Gjelder andel med status %s", logLevel = LogLevel.WARN)
        Feil graderingUtenBG(String andelStatus);

    }
}
