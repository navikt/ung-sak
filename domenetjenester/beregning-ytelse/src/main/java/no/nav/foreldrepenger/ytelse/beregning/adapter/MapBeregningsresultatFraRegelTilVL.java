package no.nav.foreldrepenger.ytelse.beregning.adapter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagUtil;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.ReferanseType;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;

@ApplicationScoped
public class MapBeregningsresultatFraRegelTilVL {

    @Inject
    public MapBeregningsresultatFraRegelTilVL() {
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
            .build(eksisterendeResultat);
        resultatPeriode.getBeregningsresultatAndelList().forEach(bra -> mapFraAndel(bra, nyPeriode));
        return nyPeriode;
    }

    private BeregningsresultatAndel mapFraAndel(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel bra, no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode brp) {
        int dagsats = BeregningsgrunnlagUtil.nullSafeLong(bra.getDagsats()).intValue();
        int dagsatsFraBg = BeregningsgrunnlagUtil.nullSafeLong(bra.getDagsatsFraBg()).intValue();
        return BeregningsresultatAndel.builder()
            .medArbeidsgiver(finnArbeidsgiver(bra))
            .medBrukerErMottaker(bra.erBrukerMottaker())
            .medDagsats(dagsats)
            .medStillingsprosent(bra.getStillingsprosent())
            .medUtbetalingsgrad(bra.getUtbetalingsgrad())
            .medDagsatsFraBg(dagsatsFraBg)
            .medAktivitetStatus(AktivitetStatusMapper.fraRegelTilVl(bra))
            .medArbeidsforholdRef(bra.getArbeidsforhold() == null
                ? null : bra.getArbeidsforhold().getArbeidsforholdId())
            .medInntektskategori(InntektskategoriMapper.fraRegelTilVL(bra.getInntektskategori()))
            .build(brp);
    }

    private Arbeidsgiver finnArbeidsgiver(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel bra) {
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
