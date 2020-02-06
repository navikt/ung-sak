package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.MatchBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagPeriodeDto;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
public class FordelRefusjonTjeneste {
    private MatchBeregningsgrunnlagTjeneste matchBeregningsgrunnlagTjeneste;

    public FordelRefusjonTjeneste() {
        // For CDI
    }

    @Inject
    public FordelRefusjonTjeneste(MatchBeregningsgrunnlagTjeneste matchBeregningsgrunnlagTjeneste) {
        this.matchBeregningsgrunnlagTjeneste = matchBeregningsgrunnlagTjeneste;
    }

    /**
     * Lager map for å fordele refusjon mellom andeler i periode
     *
     * @param behandlingId     aktuell behandlingId
     * @param fordeltPeriode periode fra dto
     * @param korrektPeriode periode fra beregningsgrunnlag
     * @return Map fra andel til refusjonsbeløp
     */
    Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> getRefusjonPrÅrMap(Long behandlingId,
                                                                           FastsettBeregningsgrunnlagPeriodeDto fordeltPeriode,
                                                                           BeregningsgrunnlagPeriode korrektPeriode) {
        Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> beløpMap = getTotalbeløpPrArbeidsforhold(behandlingId, fordeltPeriode, korrektPeriode);
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> refusjonMap = new HashMap<>();
        fordeltPeriode.getAndeler()
            .stream()
            .filter(a -> a.getArbeidsgiverId() != null)
            .forEach(fordeltAndel -> {
                var arbeidsforhold = getKorrektArbeidsforhold(behandlingId, fordeltAndel);
                fordelRefusjonTilAndel(beløpMap, refusjonMap, fordeltAndel, arbeidsforhold);
            });
        return refusjonMap;
    }

    private void fordelRefusjonTilAndel(Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> beløpMap,
                                        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> refusjonMap,
                                        FastsettBeregningsgrunnlagAndelDto fordeltAndel,
                                        BGAndelArbeidsforhold arbeidsforhold) {
        RefusjonOgFastsattBeløp refusjonOgFastsattBeløp = beløpMap.get(arbeidsforhold);
        if (refusjonOgFastsattBeløp.getTotalFastsattBeløpPrÅr().compareTo(BigDecimal.ZERO) == 0 ||
            refusjonOgFastsattBeløp.getTotalRefusjonPrÅr().compareTo(BigDecimal.ZERO) == 0) {
            if (fordeltAndel.getFastsatteVerdier().getRefusjonPrÅr() != null) {
                refusjonMap.put(fordeltAndel, BigDecimal.valueOf(fordeltAndel.getFastsatteVerdier().getRefusjonPrÅr()));
            }
            return;
        }
        BigDecimal refusjonPrÅr = getAndelAvTotalRefusjonPrÅr(fordeltAndel, refusjonOgFastsattBeløp);
        refusjonMap.put(fordeltAndel, refusjonPrÅr);
    }

    private BigDecimal getAndelAvTotalRefusjonPrÅr(FastsettBeregningsgrunnlagAndelDto fordeltAndel,
                                                   RefusjonOgFastsattBeløp refusjonOgFastsattBeløp) {
        int fastsatt = fordeltAndel.getFastsatteVerdier().finnEllerUtregnFastsattBeløpPrÅr().intValue();
        BigDecimal totalFastsatt = refusjonOgFastsattBeløp.getTotalFastsattBeløpPrÅr();
        BigDecimal totalRefusjon = refusjonOgFastsattBeløp.getTotalRefusjonPrÅr();
        return totalRefusjon.multiply(BigDecimal.valueOf(fastsatt))
            .divide(totalFastsatt, 10, RoundingMode.HALF_UP);
    }

    private Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> getTotalbeløpPrArbeidsforhold(Long behandlingId,
                                                                                              FastsettBeregningsgrunnlagPeriodeDto fordeltPeriode,
                                                                                              BeregningsgrunnlagPeriode korrektPeriode) {
        Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> arbeidsforholdRefusjonMap = new HashMap<>();
        fordeltPeriode.getAndeler()
            .stream()
            .filter(a -> a.getArbeidsgiverId() != null)
            .forEach(fordeltAndel -> {
                leggTilRefusjon(behandlingId, korrektPeriode, arbeidsforholdRefusjonMap, fordeltAndel);
                leggTilFastsattFordeling(behandlingId, arbeidsforholdRefusjonMap, fordeltAndel);
            });
        return arbeidsforholdRefusjonMap;
    }

    private void leggTilFastsattFordeling(Long behandlingId,
                                          Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> arbeidsforholdRefusjonMap,
                                          FastsettBeregningsgrunnlagAndelDto fordeltAndel) {
        BGAndelArbeidsforhold korrektArbeidsforhold = getKorrektArbeidsforhold(behandlingId, fordeltAndel);
        BigDecimal fastsattBeløpPrÅr = fordeltAndel.getFastsatteVerdier().finnEllerUtregnFastsattBeløpPrÅr();
        settEllerOppdaterFastsattBeløp(arbeidsforholdRefusjonMap, korrektArbeidsforhold, fastsattBeløpPrÅr);
    }

    private void settEllerOppdaterFastsattBeløp(Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> arbeidsforholdRefusjonMap,
                                                BGAndelArbeidsforhold arbeidsforhold, BigDecimal fastsattBeløpPrÅr) {
        if (arbeidsforholdRefusjonMap.containsKey(arbeidsforhold)) {
            RefusjonOgFastsattBeløp nyttBeløp = arbeidsforholdRefusjonMap.get(arbeidsforhold)
                .leggTilFastsattBeløp(fastsattBeløpPrÅr);
            arbeidsforholdRefusjonMap.put(arbeidsforhold, nyttBeløp);
        } else {
            arbeidsforholdRefusjonMap.put(arbeidsforhold, new RefusjonOgFastsattBeløp(BigDecimal.ZERO, fastsattBeløpPrÅr));
        }
    }

    private void leggTilRefusjon(Long behandlingId, BeregningsgrunnlagPeriode korrektPeriode, Map<BGAndelArbeidsforhold,
        RefusjonOgFastsattBeløp> arbeidsforholdRefusjonMap, FastsettBeregningsgrunnlagAndelDto fordeltAndel) {
        if (fordeltAndel.getFastsatteVerdier().getRefusjonPrÅr() == null) {
            leggTilForKunEndretFordeling(korrektPeriode, arbeidsforholdRefusjonMap, fordeltAndel);
        } else {
            leggTilForEndretFordelingOgRefusjon(behandlingId, arbeidsforholdRefusjonMap, fordeltAndel);
        }
    }

    private void leggTilForEndretFordelingOgRefusjon(Long behandlingId,
                                                     Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> arbeidsforholdRefusjonMap,
                                                     FastsettBeregningsgrunnlagAndelDto fordeltAndel) {
        var korrektArbeidsforhold = getKorrektArbeidsforhold(behandlingId, fordeltAndel);
        BigDecimal refusjonskravPrÅr = BigDecimal.valueOf(fordeltAndel.getFastsatteVerdier().getRefusjonPrÅr());
        settEllerOppdaterTotalRefusjon(arbeidsforholdRefusjonMap, korrektArbeidsforhold, refusjonskravPrÅr);
    }

    private void leggTilForKunEndretFordeling(BeregningsgrunnlagPeriode korrektPeriode,
                                              Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> arbeidsforholdRefusjonMap, FastsettBeregningsgrunnlagAndelDto fordeltAndel) {
        if (!fordeltAndel.getLagtTilAvSaksbehandler()) {
            Optional<BeregningsgrunnlagPrStatusOgAndel> korrektAndelOpt = korrektPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(andel -> andel.getAndelsnr().equals(fordeltAndel.getAndelsnr())).findFirst();
            korrektAndelOpt.ifPresent(korrektAndel ->
                leggTilRefusjonForAndelIGrunnlag(arbeidsforholdRefusjonMap, korrektAndel)
            );
        }
    }

    private void leggTilRefusjonForAndelIGrunnlag(Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> arbeidsforholdRefusjonMap, BeregningsgrunnlagPrStatusOgAndel korrektAndel) {
        korrektAndel.getBgAndelArbeidsforhold().ifPresent(arbeidsforhold -> {
            BigDecimal refusjonskravPrÅr = arbeidsforhold.getRefusjonskravPrÅr() == null ? BigDecimal.ZERO : arbeidsforhold.getRefusjonskravPrÅr();
            settEllerOppdaterTotalRefusjon(arbeidsforholdRefusjonMap, arbeidsforhold, refusjonskravPrÅr);
        });
    }

    private void settEllerOppdaterTotalRefusjon(Map<BGAndelArbeidsforhold, RefusjonOgFastsattBeløp> arbeidsforholdRefusjonMap,
                                                BGAndelArbeidsforhold arbeidsforhold, BigDecimal refusjonskravPrÅr) {
        if (arbeidsforholdRefusjonMap.containsKey(arbeidsforhold)) {
            RefusjonOgFastsattBeløp nyttBeløp = arbeidsforholdRefusjonMap.get(arbeidsforhold)
                .leggTilRefusjon(refusjonskravPrÅr);
            arbeidsforholdRefusjonMap.put(arbeidsforhold, nyttBeløp);
        } else {
            arbeidsforholdRefusjonMap.put(arbeidsforhold, new RefusjonOgFastsattBeløp(refusjonskravPrÅr));
        }
    }

    private BGAndelArbeidsforhold getKorrektArbeidsforhold(Long behandlingId, FastsettBeregningsgrunnlagAndelDto fordeltAndel) {
        var arbeidsforholdId = fordeltAndel.getArbeidsforholdId();
        var arbeidsgiverId = fordeltAndel.getArbeidsgiverId();
        return matchBeregningsgrunnlagTjeneste.matchArbeidsforholdIAktivtGrunnlag(behandlingId, arbeidsgiverId, arbeidsforholdId)
            .orElseThrow(() -> FordelRefusjonTjenesteFeil.FACTORY.fantIkkeArbeidsforhold(arbeidsgiverId, arbeidsforholdId.getReferanse()).toException());
    }

    private interface FordelRefusjonTjenesteFeil extends DeklarerteFeil {

        FordelRefusjonTjenesteFeil FACTORY = FeilFactory.create(FordelRefusjonTjenesteFeil.class);

        @TekniskFeil(feilkode = "FP-401711", feilmelding = "Fant ikke bgAndelArbeidsforhold for arbeidsgiverId %s og arbeidsforholdId %s", logLevel = LogLevel.WARN)
        Feil fantIkkeArbeidsforhold(String arbeidsgiverId, String arbeidsforholdId);
    }

}
