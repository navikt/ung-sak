package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Grunnbeløp;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * In-memory - legger kun grunnlag i minne (lagrer ikke i noe lager). (Ønsker at denne kunne blitt fjernet)
 * NB: Skal kun brukes for tester.
 * <p>
 * Definer som alternative i beans.xml (i src/test/resources/META-INF) i modul som skal bruke
 * <p>
 * <p>
 * Legg inn i fil <code>src/test/resources/META-INF</code> for å aktivere for enhetstester:
 * <p>
 */
@RequestScoped
@Alternative
public class KalkulusInMemoryTjeneste implements KalkulusApiTjeneste {

    private final Map<UUID, Deque<UUID>> indeksBehandlingTilGrunnlag = new LinkedHashMap<>();
    private final List<BeregningsgrunnlagGrunnlag> grunnlag = new ArrayList<>();

    /**
     * CDI ctor for proxies.
     */
    public KalkulusInMemoryTjeneste() {
    }

    @Override
    public SamletKalkulusResultat startBeregning(BehandlingReferanse referanse, List<StartBeregningInput> startBeregningInput) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public SamletKalkulusResultat fortsettBeregning(BehandlingReferanse behandlingReferanse, Collection<BgRef> bgReferanser, BehandlingStegType stegType) {
        throw new IllegalStateException("Skal ALDRI bli implementert");
    }

    @Override
    public List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningListe(BehandlingReferanse behandlingReferanse, Map<UUID, HåndterBeregningDto> håndterMap) {
        return List.of();
    }

    @Override
    public List<Beregningsgrunnlag> hentEksaktFastsatt(BehandlingReferanse ref, Collection<BgRef> bgReferanser) {

        Map<BgRef, Beregningsgrunnlag> resultater = new LinkedHashMap<>();
        for (var bgRef : bgReferanser) {
            var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(bgRef.getRef(), k -> new LinkedList<>());
            if (behGrunnlag.isEmpty()) {
                throw new IllegalStateException("Mangler Beregningsgrunnlag for behandling " + bgRef);
            }

            Optional<BeregningsgrunnlagGrunnlag> first = behGrunnlag.stream().map(grId -> hentGrunnlagForGrunnlagId(0L, grId))
                .filter(BeregningsgrunnlagGrunnlag::getAktiv)
                .findFirst();

            if (first.isPresent()) {
                var beregningsgrunnlagGrunnlag = first.get();
                beregningsgrunnlagGrunnlag.getBeregningsgrunnlag().ifPresent(b -> resultater.put(bgRef, b));
            } else {
                throw new IllegalStateException("Mangler Beregningsgrunnlag for behandling " + bgRef);
            }
        }

        return List.copyOf(resultater.values());
    }

    @Override
    public BeregningsgrunnlagListe hentBeregningsgrunnlagListeDto(BehandlingReferanse referanse, Set<BeregningsgrunnlagReferanse> bgReferanser) {
        return null;
    }

    @Override
    public List<BeregningsgrunnlagGrunnlag> hentGrunnlag(BehandlingReferanse ref, Collection<BgRef> bgReferanser) {
        return List.of();
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
    public void deaktiverBeregningsgrunnlag(FagsakYtelseType fagsakYtelseType, Saksnummer saksnummer, List<UUID> bgReferanse) {
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
