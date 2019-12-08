package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsperiodeTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatusV2;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.SplittetAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.SplittetPeriode;
import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;

@ApplicationScoped
public class MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering extends MapFastsettBeregningsgrunnlagPerioderFraRegelTilVL {

    private static Map<AktivitetStatusV2, AktivitetStatus> statusMap = new EnumMap<>(AktivitetStatusV2.class);
    private static Map<AktivitetStatus, OpptjeningAktivitetType> aktivitetTypeMap = new HashMap<>();

    static {
        statusMap.put(AktivitetStatusV2.SN, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        statusMap.put(AktivitetStatusV2.FL, AktivitetStatus.FRILANSER);
        aktivitetTypeMap.put(AktivitetStatus.FRILANSER, OpptjeningAktivitetType.FRILANS);
        aktivitetTypeMap.put(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, OpptjeningAktivitetType.NÆRING);
    }

    @Inject
    public MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering() {
        super();
    }

    @Override
    protected void mapAndeler(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, SplittetPeriode splittetPeriode,
                              List<BeregningsgrunnlagPrStatusOgAndel> andelListe, BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        andelListe.forEach(eksisterendeAndel -> mapEksisterendeAndel(splittetPeriode, beregningsgrunnlagPeriode, eksisterendeAndel));
        splittetPeriode.getNyeAndeler()
            .forEach(nyAndel -> mapNyAndelTaHensynTilSNFL(beregningsgrunnlagPeriode, nyttBeregningsgrunnlag.getSkjæringstidspunkt(), nyAndel));
    }

    private void mapNyAndelTaHensynTilSNFL(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, LocalDate skjæringstidspunkt, SplittetAndel nyAndel) {
        DatoIntervallEntitet beregningsperiode;
        if (nyAndel.getAktivitetStatus() != null && AktivitetStatusV2.SN.equals(nyAndel.getAktivitetStatus())) {
            beregningsperiode = BeregningsperiodeTjeneste.fastsettBeregningsperiodeForSNAndeler(skjæringstidspunkt);
        } else {
            beregningsperiode = BeregningsperiodeTjeneste.fastsettBeregningsperiodeForATFLAndeler(skjæringstidspunkt);
        }
        if (nyAndelErSNEllerFl(nyAndel)) {
            AktivitetStatus aktivitetStatus = mapAktivitetStatus(nyAndel.getAktivitetStatus());
            if (aktivitetStatus == null) {
                throw new IllegalStateException("Klarte ikke identifisere aktivitetstatus under periodesplitt. Status var " + nyAndel.getAktivitetStatus());
            }
            boolean eksisterende = beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .anyMatch(a -> a.getAktivitetStatus().equals(aktivitetStatus) && a.getArbeidsforholdType().equals(aktivitetTypeMap.get(aktivitetStatus)) &&
                    a.getBeregningsperiodeFom().equals(beregningsperiode.getFomDato()) && a.getBeregningsperiodeTom().equals(beregningsperiode.getTomDato()));
            if (!eksisterende) {
                BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medAktivitetStatus(aktivitetStatus)
                    .medArbforholdType(aktivitetTypeMap.get(aktivitetStatus))
                    .medBeregningsperiode(beregningsperiode.getFomDato(), beregningsperiode.getTomDato())
                    .build(beregningsgrunnlagPeriode);
            }
        } else {
            Arbeidsgiver arbeidsgiver = MapArbeidsforholdFraRegelTilVL.map(nyAndel.getArbeidsforhold());
            InternArbeidsforholdRef iaRef = InternArbeidsforholdRef.ref(nyAndel.getArbeidsforhold().getArbeidsforholdId());
            BGAndelArbeidsforhold.Builder andelArbeidsforholdBuilder = BGAndelArbeidsforhold.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdRef(iaRef)
                .medArbeidsperiodeFom(nyAndel.getArbeidsperiodeFom())
                .medArbeidsperiodeTom(nyAndel.getArbeidsperiodeTom())
                .medRefusjonskravPrÅr(nyAndel.getRefusjonskravPrÅr());
            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(andelArbeidsforholdBuilder)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medArbforholdType(OpptjeningAktivitetType.ARBEID)
                .medBeregningsperiode(beregningsperiode.getFomDato(), beregningsperiode.getTomDato())
                .build(beregningsgrunnlagPeriode);
        }
    }

    private AktivitetStatus mapAktivitetStatus(AktivitetStatusV2 aktivitetStatusV2) {
        if (aktivitetStatusV2 == null) {
            return null;
        }
        var status = statusMap.get(aktivitetStatusV2);
        if (status == null) {
            throw new IllegalStateException(
                "Mangler mapping til " + AktivitetStatus.class.getName() + " fra " + AktivitetStatusV2.class.getName() + "." + aktivitetStatusV2);
        }
        return status;
    }

    private boolean nyAndelErSNEllerFl(SplittetAndel nyAndel) {
        return nyAndel.getAktivitetStatus() != null
            && (nyAndel.getAktivitetStatus().equals(AktivitetStatusV2.SN) || nyAndel.getAktivitetStatus().equals(AktivitetStatusV2.FL));
    }

    private void mapEksisterendeAndel(SplittetPeriode splittetPeriode, BeregningsgrunnlagPeriode beregningsgrunnlagPeriode,
                                      BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel) {
        BeregningsgrunnlagPrStatusOgAndel nyAndel = Kopimaskin.deepCopy(eksisterendeAndel);
        BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder(nyAndel);
        Optional<BeregningsgrunnlagPrArbeidsforhold> regelMatchOpt = finnEksisterendeAndelFraRegel(splittetPeriode, eksisterendeAndel);
        regelMatchOpt.ifPresent(regelAndel -> {
            BGAndelArbeidsforhold andelArbeidsforhold = nyAndel.getBgAndelArbeidsforhold().orElseThrow();
            BGAndelArbeidsforhold.Builder andelArbeidsforholdBuilder = BGAndelArbeidsforhold.builder(andelArbeidsforhold)
                .medRefusjonskravPrÅr(regelAndel.getRefusjonskravPrÅr().orElse(BigDecimal.ZERO));
            andelBuilder.medBGAndelArbeidsforhold(andelArbeidsforholdBuilder);
        });

        andelBuilder
            .build(beregningsgrunnlagPeriode);
    }

}
