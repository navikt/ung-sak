package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;

/**
 * In-memory - legger kun grunnlag i minne (lagrer ikke i noe lager). (Ønsker at denne kunne blitt fjernet)
 * NB: Skal kun brukes for tester.
 * <p>
 * Definer som alternative i beans.xml (i src/test/resources/META-INF) i modul som skal bruke<p>
 * <p>
 * Legg inn i fil <code>src/test/resources/META-INF</code> for å aktivere for enhetstester: <p>
 */
@RequestScoped
@Alternative
public class KalkulusInMermoryTjeneste implements BeregningTjeneste {

    private final Map<Long, Deque<UUID>> indeksBehandlingTilGrunnlag = new LinkedHashMap<>();
    private final List<BeregningsgrunnlagGrunnlag> grunnlag = new ArrayList<>();

    /**
     * CDI ctor for proxies.
     */
    public KalkulusInMermoryTjeneste() {
    }

    @Override
    public List<BeregningAksjonspunktResultat> startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public List<BeregningAksjonspunktResultat> fortsettBeregning(BehandlingReferanse referanse, BehandlingStegType stegType) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public List<BeregningAksjonspunktResultat> oppdaterBeregning(HåndterBeregningDto håndterBeregningDto, BehandlingReferanse referanse) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public Optional<Beregningsgrunnlag> hentFastsatt(Long behandlingId) {
        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(behandlingId, k -> new LinkedList<>());
        if (behGrunnlag.isEmpty()) {
            return Optional.empty();
        }

        Optional<BeregningsgrunnlagGrunnlag> first = behGrunnlag.stream().map(grId -> hentGrunnlagForGrunnlagId(behandlingId, grId))
                .filter(BeregningsgrunnlagGrunnlag::getAktiv)
                .findFirst();

        if (first.isPresent()) {

            BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag = first.get();
            return beregningsgrunnlagGrunnlag.getBeregningsgrunnlag();
        }
        return Optional.empty();
    }

    @Override
    public Beregningsgrunnlag hentEksaktFastsatt(Long behandlingId) {
        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(behandlingId, k -> new LinkedList<>());
        if (behGrunnlag.isEmpty()) {
            throw new IllegalStateException("Mangler Beregningsgrunnlag for behandling " + behandlingId);
        }

        Optional<BeregningsgrunnlagGrunnlag> first = behGrunnlag.stream().map(grId -> hentGrunnlagForGrunnlagId(behandlingId, grId))
                .filter(BeregningsgrunnlagGrunnlag::getAktiv)
                .findFirst();

        if (first.isPresent()) {
            BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag = first.get();
            return beregningsgrunnlagGrunnlag.getBeregningsgrunnlag().orElseThrow(() -> new IllegalStateException("Mangler Beregningsgrunnlag for behandling " + behandlingId));
        }
        throw new IllegalStateException("Mangler Beregningsgrunnlag for behandling " + behandlingId);
    }

    @Override
    public BeregningsgrunnlagDto hentBeregningsgrunnlagDto(Long behandlingId) {
        return null;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hentGrunnlag(Long behandlingId) {
        return Optional.empty();
    }

    @Override
    public void lagreBeregningsgrunnlag(Long behandlingId, Beregningsgrunnlag beregningsgrunnlag, BeregningsgrunnlagTilstand tilstand) {
        BeregningsgrunnlagGrunnlagBuilder oppdatere = BeregningsgrunnlagGrunnlagBuilder.oppdatere(getAktivtInntektArbeidGrunnlag(behandlingId));
        oppdatere.medBeregningsgrunnlag(beregningsgrunnlag);
        BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag = oppdatere.build(tilstand);

        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(behandlingId, k -> new LinkedList<>());

        behGrunnlag.push(beregningsgrunnlagGrunnlag.getEksternReferanse());
        grunnlag.add(beregningsgrunnlagGrunnlag);
    }

    @Override
    public Optional<Beregningsgrunnlag> hentBeregningsgrunnlagForId(UUID uuid, Long behandlingId) {
        return Optional.empty();
    }

    @Override
    public void deaktiverBeregningsgrunnlag(Long behandlingId) {
    }

    @Override
    public Boolean erEndringIBeregning(Long behandlingId1, Long behandlingId2) {
        return false;
    }


    private Optional<BeregningsgrunnlagGrunnlag> getAktivtInntektArbeidGrunnlag(Long behandlingId) {
        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(behandlingId, k -> new LinkedList<>());
        if (behGrunnlag.isEmpty()) {
            return Optional.empty();
        }
        return behGrunnlag.stream().map(grId -> hentGrunnlagForGrunnlagId(behandlingId, grId))
                .filter(BeregningsgrunnlagGrunnlag::getAktiv)
                .findFirst();
    }

    public BeregningsgrunnlagGrunnlag hentGrunnlagForGrunnlagId(Long behandlingId, UUID inntektArbeidYtelseGrunnlagId) {
        return grunnlag.stream().filter(g -> Objects.equals(g.getEksternReferanse(), inntektArbeidYtelseGrunnlagId))
                .findFirst().orElseThrow();
    }
}

