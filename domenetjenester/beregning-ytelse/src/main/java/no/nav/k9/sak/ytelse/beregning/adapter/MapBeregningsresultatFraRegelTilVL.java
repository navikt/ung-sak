package no.nav.k9.sak.ytelse.beregning.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagUtil;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.ReferanseType;

@ApplicationScoped
public class MapBeregningsresultatFraRegelTilVL {

    private boolean brukUtbetalingsgradOppdrag;

    public MapBeregningsresultatFraRegelTilVL() {
        //for CDI proxy
    }

    @Inject
    public MapBeregningsresultatFraRegelTilVL(@KonfigVerdi(value = "ENABLE_UTBETALINGSGRAD_OPPDRAG", defaultVerdi = "false") boolean brukUtbetalingsgradOppdrag) {
        this.brukUtbetalingsgradOppdrag = brukUtbetalingsgradOppdrag;
    }

    public no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet mapFra(Beregningsresultat resultat, no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet eksisterendeResultat) {
        if (eksisterendeResultat.getBeregningsresultatPerioder().isEmpty()) {
            resultat.getBeregningsresultatPerioder().forEach(p -> mapFraPeriode(p, eksisterendeResultat));
        } else {
            throw new IllegalArgumentException("Forventer ingen beregningsresultatPerioder");
        }
        return eksisterendeResultat;
    }

    private no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode mapFraPeriode(BeregningsresultatPeriode resultatPeriode, no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet eksisterendeResultat) {
        no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode nyPeriode = no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(resultatPeriode.getFom(), resultatPeriode.getTom())
            .medInntektGraderingprosent(resultatPeriode.getInntektGraderingsprosent())
            .medTotalUtbetalingsgradFraUttak(resultatPeriode.getTotalUtbetalingsgradFraUttak())
            .medTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt(resultatPeriode.getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt())
            .medReduksjonsfaktorInaktivTypeA(resultatPeriode.getReduksjonsfaktorInaktivTypeA())
            .medGraderingsfaktorInntekt(resultatPeriode.getGraderingsfaktorInntekt())
            .medGraderingsfaktorTid(resultatPeriode.getGraderingsfaktorTid())
            .build(eksisterendeResultat);
        resultatPeriode.getBeregningsresultatAndelList().forEach(bra -> mapFraAndel(bra, nyPeriode, resultatPeriode.getPeriode()));
        return nyPeriode;
    }

    private BeregningsresultatAndel mapFraAndel(no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel bra,
                                                no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode brp, LocalDateInterval periode) {
        int dagsats = BeregningsgrunnlagUtil.nullSafeLong(bra.getDagsats()).intValue();
        int dagsatsFraBg = BeregningsgrunnlagUtil.nullSafeLong(bra.getDagsatsFraBg()).intValue();
        return BeregningsresultatAndel.builder()
            .medArbeidsgiver(finnArbeidsgiver(bra))
            .medBrukerErMottaker(bra.erBrukerMottaker())
            .medDagsats(dagsats)
            .medStillingsprosent(bra.getStillingsprosent())
            .medUtbetalingsgrad(bra.getUtbetalingsgrad())
            .medUtbetalingsgradOppdrag(brukUtbetalingsgradOppdrag ? bra.getUtbetalingsgradOppdrag() : null)
            .medPeriode(periode)
            .medDagsatsFraBg(dagsatsFraBg)
            .medAktivitetStatus(AktivitetStatusMapper.fraRegelTilVl(bra))
            .medArbeidsforholdRef(bra.getArbeidsforhold() == null
                ? null : bra.getArbeidsforhold().getArbeidsforholdId())
            .medInntektskategori(InntektskategoriMapper.fraRegelTilVL(bra.getInntektskategori()))
            .buildFor(brp);
    }

    private Arbeidsgiver finnArbeidsgiver(no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel bra) {
        if (bra.getArbeidsforhold() == null) {
            return null;
        } else {
            String identifikator = bra.getArbeidsforhold().getIdentifikator();
            ReferanseType referanseType = bra.getArbeidsforhold().getReferanseType();
            if (referanseType == ReferanseType.AKTØR_ID) {
                return Arbeidsgiver.person(new AktørId(identifikator));
            } else if (referanseType == ReferanseType.ORG_NR) {
                return Arbeidsgiver.virksomhet(identifikator);
            }
        }
        return null;
    }
}
