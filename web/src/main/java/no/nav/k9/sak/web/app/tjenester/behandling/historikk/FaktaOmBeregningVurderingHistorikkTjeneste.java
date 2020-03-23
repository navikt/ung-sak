package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import static no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType.ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD;
import static no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType.FRILANSVIRKSOMHET;
import static no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType.LØNNSENDRING_I_PERIODEN;
import static no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType.MILITÆR_ELLER_SIVIL;
import static no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType.MOTTAR_YTELSE_ARBEID;
import static no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType.MOTTAR_YTELSE_FRILANS;
import static no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType.NY_REFUSJONSFRIST;
import static no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType.SELVSTENDIG_NÆRINGSDRIVENDE;
import static no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType.VURDER_ETTERLØNN_SLUTTPAKKE;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.output.ErMottattYtelseEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.output.ErTidsbegrensetArbeidsforholdEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.output.FaktaOmBeregningVurderinger;
import no.nav.folketrygdloven.beregningsgrunnlag.output.RefusjonskravGyldighetEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.output.ToggleEndring;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;

/**
 * Lager historikk for radioknapp-vurderinger i fakta om beregning.
 */
@ApplicationScoped
public class FaktaOmBeregningVurderingHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public FaktaOmBeregningVurderingHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FaktaOmBeregningVurderingHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag,
                                                      InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void lagHistorikkForVurderinger(Long behandlingId, HistorikkInnslagTekstBuilder tekstBuilder, FaktaOmBeregningVurderinger faktaOmBeregningVurderinger) {
        faktaOmBeregningVurderinger.getErNyoppstartetFLEndring()
            .ifPresent(toggleEndring -> lagFaktaVurderingInnslag(tekstBuilder, FRILANSVIRKSOMHET, toggleEndring, this::konvertBooleanTilNyoppstartetFLVerdiType));

        faktaOmBeregningVurderinger.getErSelvstendingNyIArbeidslivetEndring()
            .ifPresent(toggleEndring -> lagFaktaVurderingInnslag(tekstBuilder, SELVSTENDIG_NÆRINGSDRIVENDE, toggleEndring, this::konvertBooleanTilNyIarbeidslivetVerdiType));

        faktaOmBeregningVurderinger.getHarLønnsendringIBeregningsperiodenEndring()
            .ifPresent(toggleEndring -> tekstBuilder.medEndretFelt(LØNNSENDRING_I_PERIODEN, toggleEndring.getFraVerdi(), toggleEndring.getTilVerdi()));

        faktaOmBeregningVurderinger.getHarMilitærSiviltjenesteEndring()
            .ifPresent(toggleEndring -> tekstBuilder.medEndretFelt(MILITÆR_ELLER_SIVIL, toggleEndring.getFraVerdi(), toggleEndring.getTilVerdi()));

        faktaOmBeregningVurderinger.getHarEtterlønnSluttpakkeEndring()
            .ifPresent(toggleEndring -> tekstBuilder.medEndretFelt(VURDER_ETTERLØNN_SLUTTPAKKE, toggleEndring.getFraVerdi(), toggleEndring.getTilVerdi()));

        lagHistorikkForErMottattYtelseEndringer(behandlingId,
            tekstBuilder,
            faktaOmBeregningVurderinger.getErMottattYtelseEndringer());

        lagHistorikkForTidsbegrensetArbeidsforholdEndringer(behandlingId,
            tekstBuilder,
            faktaOmBeregningVurderinger.getErTidsbegrensetArbeidsforholdEndringer());

        lagHistorikkForRefusjonGyldighetEndringer(behandlingId,
            tekstBuilder,
            faktaOmBeregningVurderinger.getVurderRefusjonskravGyldighetEndringer());
    }

    private void lagHistorikkForTidsbegrensetArbeidsforholdEndringer(Long behandlingId, HistorikkInnslagTekstBuilder tekstBuilder, List<ErTidsbegrensetArbeidsforholdEndring> erTidsbegrensetArbeidsforholdEndringer) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        erTidsbegrensetArbeidsforholdEndringer.forEach(erTidsbegrensetArbeidsforholdEndring -> {
            ToggleEndring endring = erTidsbegrensetArbeidsforholdEndring.getErTidsbegrensetArbeidsforholdEndring();
            String info = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(erTidsbegrensetArbeidsforholdEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
            lagFaktaVurderingInnslag(tekstBuilder, ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD, info, endring, this::konvertBooleanTilErTidsbegrensetVerdiType);
        });
    }

    private void lagHistorikkForErMottattYtelseEndringer(Long behandlingId, HistorikkInnslagTekstBuilder tekstBuilder, List<ErMottattYtelseEndring> erMottattYtelseEndringer) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        erMottattYtelseEndringer.forEach(erMottattYtelseEndring -> {
            ToggleEndring endring = erMottattYtelseEndring.getErMottattYtelseEndring();
            if (erMottattYtelseEndring.getArbeidsgiver() != null) {
                String info = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(erMottattYtelseEndring.getArbeidsgiver(), arbeidsforholdOverstyringer);
                tekstBuilder.medEndretFelt(MOTTAR_YTELSE_ARBEID,
                    info,
                    endring.getFraVerdi(),
                    endring.getTilVerdi());
            } else if (erMottattYtelseEndring.getAktivitetStatus().erFrilanser()) {
                tekstBuilder.medEndretFelt(MOTTAR_YTELSE_FRILANS,
                    endring.getFraVerdi(),
                    endring.getTilVerdi());
            }
        });
    }

    private void lagHistorikkForRefusjonGyldighetEndringer(Long behandlingId, HistorikkInnslagTekstBuilder tekstBuilder, List<RefusjonskravGyldighetEndring> refusjonskravGyldighetEndringer) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        refusjonskravGyldighetEndringer.forEach(refusjonskravGyldighetEndring -> {
            ToggleEndring erGyldighetUtvidet = refusjonskravGyldighetEndring.getErGyldighetUtvidet();
            tekstBuilder.medEndretFelt(NY_REFUSJONSFRIST,
                arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(refusjonskravGyldighetEndring.getArbeidsgiver(), arbeidsforholdOverstyringer),
                erGyldighetUtvidet.getFraVerdi(), erGyldighetUtvidet.getTilVerdi());
        });
    }

    private void lagFaktaVurderingInnslag(HistorikkInnslagTekstBuilder tekstBuilder,
                                          HistorikkEndretFeltType endretFeltType,
                                          String info,
                                          ToggleEndring endring,
                                          KonverterBoolenTilVerdiType konverterer) {
        HistorikkEndretFeltVerdiType opprinneligVerdi = konverterer.konverter(endring.getFraVerdi());
        HistorikkEndretFeltVerdiType nyVerdi = konverterer.konverter(endring.getTilVerdi());
        tekstBuilder.medEndretFelt(endretFeltType, info, opprinneligVerdi, nyVerdi);
    }

    private void lagFaktaVurderingInnslag(HistorikkInnslagTekstBuilder tekstBuilder,
                                          HistorikkEndretFeltType endretFeltType,
                                          ToggleEndring endring,
                                          KonverterBoolenTilVerdiType konverterer) {
        HistorikkEndretFeltVerdiType opprinneligVerdi = konverterer.konverter(endring.getFraVerdi());
        HistorikkEndretFeltVerdiType nyVerdi = konverterer.konverter(endring.getTilVerdi());
        tekstBuilder.medEndretFelt(endretFeltType, opprinneligVerdi, nyVerdi);
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilErTidsbegrensetVerdiType(Boolean endringTidsbegrensetArbeidsforhold) {
        if (endringTidsbegrensetArbeidsforhold == null) {
            return null;
        }
        return endringTidsbegrensetArbeidsforhold ? HistorikkEndretFeltVerdiType.TIDSBEGRENSET_ARBEIDSFORHOLD : HistorikkEndretFeltVerdiType.IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilNyIarbeidslivetVerdiType(Boolean erNyIArbeidslivet) {
        if (erNyIArbeidslivet == null) {
            return null;
        }
        return erNyIArbeidslivet ? HistorikkEndretFeltVerdiType.NY_I_ARBEIDSLIVET : HistorikkEndretFeltVerdiType.IKKE_NY_I_ARBEIDSLIVET;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilNyoppstartetFLVerdiType(Boolean erNyoppstartet) {
        if (erNyoppstartet == null) {
            return null;
        }
        return erNyoppstartet ? HistorikkEndretFeltVerdiType.NYOPPSTARTET : HistorikkEndretFeltVerdiType.IKKE_NYOPPSTARTET;
    }

    @FunctionalInterface
    interface KonverterBoolenTilVerdiType {
        HistorikkEndretFeltVerdiType konverter(Boolean verdi);
    }

}
