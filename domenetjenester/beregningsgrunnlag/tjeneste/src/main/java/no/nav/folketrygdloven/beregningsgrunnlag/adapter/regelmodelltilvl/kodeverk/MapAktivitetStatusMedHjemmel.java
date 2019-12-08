package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk;

import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Hjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatusMedHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;

public class MapAktivitetStatusMedHjemmel {



    private MapAktivitetStatusMedHjemmel() {
        // skjul public constructor
    }

    public static BeregningsgrunnlagEntitet mapAktivitetStatusMedHjemmel(List<AktivitetStatusMedHjemmel> aktivitetStatuser,
                                                                  BeregningsgrunnlagEntitet eksisterendeVLGrunnlag, BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        for (no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatusMedHjemmel regelStatus : aktivitetStatuser) {
            AktivitetStatus modellStatus = fraRegel(regelStatus.getAktivitetStatus(), beregningsgrunnlagPeriode);
            Hjemmel hjemmel = MapHjemmelFraRegelTilVL.map(regelStatus.getHjemmel());
            BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(modellStatus).medHjemmel(hjemmel).build(eksisterendeVLGrunnlag);
        }
        return eksisterendeVLGrunnlag;
    }

    private static AktivitetStatus fraRegel(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus aktivitetStatus, no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        if (MapAktivitetStatusVedSkjæringstidspunktFraRegelTilVL.contains(aktivitetStatus)) {
            return MapAktivitetStatusVedSkjæringstidspunktFraRegelTilVL.map(aktivitetStatus);
        }
        BeregningsgrunnlagPrStatus atfl = beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus.ATFL);
        if (atfl == null) {
            return AktivitetStatus.ARBEIDSTAKER;
        }
        boolean frilanser = atfl.getFrilansArbeidsforhold().isPresent();
        boolean arbeidstaker = !atfl.getArbeidsforholdIkkeFrilans().isEmpty();

        if (no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus.ATFL.equals(aktivitetStatus)) {
            return mapATFL(frilanser, arbeidstaker);
        } else if (no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus.ATFL_SN.equals(aktivitetStatus)) {
            return mapATFL_SN(frilanser, arbeidstaker);
        }
        return MapAktivitetStatusVedSkjæringstidspunktFraRegelTilVL.map(aktivitetStatus);
    }

    private static AktivitetStatus mapATFL_SN(boolean frilanser, boolean arbeidstaker) {
        if (frilanser) {
            if (arbeidstaker) {
                return AktivitetStatus.KOMBINERT_AT_FL_SN;
            } else {
                return AktivitetStatus.KOMBINERT_FL_SN;
            }
        } else {
            return AktivitetStatus.KOMBINERT_AT_SN;
        }
    }

    private static AktivitetStatus mapATFL(boolean frilanser, boolean arbeidstaker) {
        if (frilanser) {
            if (arbeidstaker) {
                return AktivitetStatus.KOMBINERT_AT_FL;
            } else {
                return AktivitetStatus.FRILANSER;
            }
        } else {
            return AktivitetStatus.ARBEIDSTAKER;
        }
    }
}
