package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Grunnbeløp;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.behandling.BehandlingReferanse;

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
public class KalkulusInMermoryTjeneste implements KalkulusApiTjeneste {

    private final Map<UUID, Deque<UUID>> indeksBehandlingTilGrunnlag = new LinkedHashMap<>();
    private final List<BeregningsgrunnlagGrunnlag> grunnlag = new ArrayList<>();

    /**
     * CDI ctor for proxies.
     */
    public KalkulusInMermoryTjeneste() {
    }

    @Override
    public KalkulusResultat startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, UUID bgReferanse, LocalDate skjæringstidspunkt) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public KalkulusResultat fortsettBeregning(FagsakYtelseType fagsakYtelseType, UUID behandlingUuid, BehandlingStegType stegType) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public OppdaterBeregningsgrunnlagResultat oppdaterBeregning(HåndterBeregningDto håndterBeregningDto, UUID behandlingUuid) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public Optional<Beregningsgrunnlag> hentFastsatt(UUID bgReferanse, FagsakYtelseType fagsakYtelseType) {
        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(bgReferanse, k -> new LinkedList<>());
        if (behGrunnlag.isEmpty()) {
            return Optional.empty();
        }

        Optional<BeregningsgrunnlagGrunnlag> first = behGrunnlag.stream().map(grId -> hentGrunnlagForGrunnlagId(0L, grId))
            .filter(BeregningsgrunnlagGrunnlag::getAktiv)
            .findFirst();

        if (first.isPresent()) {

            BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag = first.get();
            return beregningsgrunnlagGrunnlag.getBeregningsgrunnlag();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Beregningsgrunnlag> hentEksaktFastsatt(FagsakYtelseType fagsakYtelseType, UUID bgReferanse) {
        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(bgReferanse, k -> new LinkedList<>());
        if (behGrunnlag.isEmpty()) {
            throw new IllegalStateException("Mangler Beregningsgrunnlag for behandling " + bgReferanse);
        }

        Optional<BeregningsgrunnlagGrunnlag> first = behGrunnlag.stream().map(grId -> hentGrunnlagForGrunnlagId(0L, grId))
            .filter(BeregningsgrunnlagGrunnlag::getAktiv)
            .findFirst();

        if (first.isPresent()) {
            BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag = first.get();
            return beregningsgrunnlagGrunnlag.getBeregningsgrunnlag();
        }
        throw new IllegalStateException("Mangler Beregningsgrunnlag for behandling " + bgReferanse);
    }

    @Override
    public BeregningsgrunnlagListe hentBeregningsgrunnlagListeDto(BehandlingReferanse referanse, Set<BeregningsgrunnlagReferanse> bgReferanser) {
        return null;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hentGrunnlag(FagsakYtelseType fagsakYtelseType, UUID uuid) {
        return Optional.empty();
    }

    @Override
    public void lagreBeregningsgrunnlag(BehandlingReferanse referanse, Beregningsgrunnlag beregningsgrunnlag, BeregningsgrunnlagTilstand tilstand) {
        BeregningsgrunnlagGrunnlagBuilder oppdatere = BeregningsgrunnlagGrunnlagBuilder.oppdatere(getAktivtInntektArbeidGrunnlag(referanse.getBehandlingId()));
        oppdatere.medBeregningsgrunnlag(beregningsgrunnlag);
        BeregningsgrunnlagGrunnlag beregningsgrunnlagGrunnlag = oppdatere.build(tilstand);

        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(referanse.getBehandlingUuid(), k -> new LinkedList<>());

        behGrunnlag.push(beregningsgrunnlagGrunnlag.getEksternReferanse());
        grunnlag.add(beregningsgrunnlagGrunnlag);
    }

    @Override
    public void deaktiverBeregningsgrunnlag(FagsakYtelseType fagsakYtelseType, UUID bgReferanse) {
    }

    @Override
    public Boolean erEndringIBeregning(FagsakYtelseType fagsakYtelseType1, UUID bgRefeanse1, FagsakYtelseType fagsakYtelseType2, UUID bgReferanse2) {
        return false;
    }

    @Override
    public Grunnbeløp hentGrunnbeløp(LocalDate dato) {
        return null;
    }


    private Optional<BeregningsgrunnlagGrunnlag> getAktivtInntektArbeidGrunnlag(Long behandlingId) {
        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(UUID.randomUUID(), k -> new LinkedList<>());
        if (behGrunnlag.isEmpty()) {
            return Optional.empty();
        }
        return behGrunnlag.stream().map(grId -> hentGrunnlagForGrunnlagId(behandlingId, grId))
            .filter(BeregningsgrunnlagGrunnlag::getAktiv)
            .findFirst();
    }

    public BeregningsgrunnlagGrunnlag hentGrunnlagForGrunnlagId(@SuppressWarnings("unused") Long behandlingId, UUID inntektArbeidYtelseGrunnlagId) {
        return grunnlag.stream().filter(g -> Objects.equals(g.getEksternReferanse(), inntektArbeidYtelseGrunnlagId))
            .findFirst().orElseThrow();
    }
}

