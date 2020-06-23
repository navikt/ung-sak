package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
@FagsakYtelseTypeRef("*")
public class DefaultErEndringIBeregningTjeneste implements ErEndringIBeregningVurderer {

    protected BeregningTjeneste kalkulusTjeneste;

    public DefaultErEndringIBeregningTjeneste() {
        // CDI
    }

    @Inject
    public DefaultErEndringIBeregningTjeneste(BeregningTjeneste kalkulusTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public boolean vurderUgunst(BehandlingReferanse orginalBeregning, BehandlingReferanse revurdering, LocalDate skjæringstidspuntk) {
        var originaltGrunnlag = kalkulusTjeneste.hentFastsatt(orginalBeregning, skjæringstidspuntk);
        var revurderingsGrunnlag = kalkulusTjeneste.hentFastsatt(revurdering, skjæringstidspuntk);

        return vurderUgunst(revurderingsGrunnlag, originaltGrunnlag);
    }

    public boolean vurderUgunst(Optional<Beregningsgrunnlag> revurderingsGrunnlag, Optional<Beregningsgrunnlag> originaltGrunnlag) {
        if (revurderingsGrunnlag.isEmpty()) {
            return originaltGrunnlag.isPresent();
        }

        List<BeregningsgrunnlagPeriode> originalePerioder = originaltGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());
        List<BeregningsgrunnlagPeriode> revurderingsPerioder = revurderingsGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());

        Set<LocalDate> allePeriodeDatoer = finnAllePeriodersStartdatoer(revurderingsPerioder, originalePerioder);

        for (LocalDate dato : allePeriodeDatoer) {
            Long dagsatsRevurderingsgrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, revurderingsPerioder);
            Long dagsatsOriginaltGrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, originalePerioder);
            if (dagsatsOriginaltGrunnlag != null && (dagsatsRevurderingsgrunnlag == null || dagsatsRevurderingsgrunnlag < dagsatsOriginaltGrunnlag)) {
                return true;
            }
        }
        return false;
    }

    private static Set<LocalDate> finnAllePeriodersStartdatoer(List<BeregningsgrunnlagPeriode> revurderingsPerioder, List<BeregningsgrunnlagPeriode> originalePerioder) {
        Set<LocalDate> startDatoer = new HashSet<>();
        revurderingsPerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom).forEach(startDatoer::add);
        originalePerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom).forEach(startDatoer::add);
        return startDatoer;
    }

    private static Long finnGjeldendeDagsatsForDenneDatoen(LocalDate dato, List<BeregningsgrunnlagPeriode> perioder) {
        // Hvis dato er før starten på den første perioden bruker vi første periodes dagsats
        Optional<BeregningsgrunnlagPeriode> førsteKronologiskePeriode = perioder.stream()
            .min(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom));
        if (førsteKronologiskePeriode.filter(periode -> dato.equals(periode.getBeregningsgrunnlagPeriodeFom())).isPresent()) {
            return førsteKronologiskePeriode.get().getDagsats();
        }
        if (førsteKronologiskePeriode.filter(periode -> dato.isBefore(periode.getBeregningsgrunnlagPeriodeFom())).isPresent()) {
            return førsteKronologiskePeriode.get().getDagsats();
        }
        for (BeregningsgrunnlagPeriode periode : perioder) {
            if (periode.getPeriode().inkluderer(dato)) {
                return periode.getDagsats();
            }
        }
        return 0L; // Antar her ingen endring (ved ukjent periode etc)
    }
}
