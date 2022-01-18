package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

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
    public Map<LocalDate, Boolean> vurderUgunst(BehandlingReferanse original, BehandlingReferanse revurdering, NavigableSet<LocalDate> skjæringstidspunkter) {
        if (skjæringstidspunkter.isEmpty()) {
            return Collections.emptyMap();
        }
        var originaltGrunnlag = kalkulusTjeneste.hentEksaktFastsatt(original, skjæringstidspunkter).stream().collect(Collectors.toMap(Beregningsgrunnlag::getSkjæringstidspunkt, v -> v));
        var revurderingsGrunnlag = kalkulusTjeneste.hentEksaktFastsatt(revurdering, skjæringstidspunkter).stream().collect(Collectors.toMap(Beregningsgrunnlag::getSkjæringstidspunkt, v -> v));

        var map = new TreeMap<LocalDate, Boolean>();
        for (var stp : skjæringstidspunkter) {
            boolean vurderUgunst = vurderUgunst(revurderingsGrunnlag.get(stp), originaltGrunnlag.get(stp));
            map.put(stp, vurderUgunst);
        }

        return Collections.unmodifiableMap(map);
    }

    public boolean vurderUgunst(Beregningsgrunnlag revurderingsGrunnlag, Beregningsgrunnlag originaltGrunnlag) {
        if (revurderingsGrunnlag == null) {
            return originaltGrunnlag != null;
        }

        var originalePerioder = originaltGrunnlag.getBeregningsgrunnlagPerioder();
        var revurderingsPerioder = revurderingsGrunnlag.getBeregningsgrunnlagPerioder();

        var allePeriodeDatoer = finnAllePeriodersStartdatoer(revurderingsPerioder, originalePerioder);

        for (var dato : allePeriodeDatoer) {
            Long dagsatsRevurderingsgrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, revurderingsPerioder);
            Long dagsatsOriginaltGrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, originalePerioder);
            boolean dagsatsRevurderingErMindre = (dagsatsOriginaltGrunnlag != null && (dagsatsRevurderingsgrunnlag == null || dagsatsRevurderingsgrunnlag < dagsatsOriginaltGrunnlag));
            if (dagsatsRevurderingErMindre) {
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
        var førsteKronologiskePeriode = perioder.stream().min(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom));
        if (førsteKronologiskePeriode.filter(periode -> dato.equals(periode.getBeregningsgrunnlagPeriodeFom())).isPresent()) {
            return førsteKronologiskePeriode.get().getDagsats();
        }
        if (førsteKronologiskePeriode.filter(periode -> dato.isBefore(periode.getBeregningsgrunnlagPeriodeFom())).isPresent()) {
            return førsteKronologiskePeriode.get().getDagsats();
        }
        for (var periode : perioder) {
            if (periode.getPeriode().inkluderer(dato)) {
                return periode.getDagsats();
            }
        }
        return 0L; // Antar her ingen endring (ved ukjent periode etc)
    }
}
