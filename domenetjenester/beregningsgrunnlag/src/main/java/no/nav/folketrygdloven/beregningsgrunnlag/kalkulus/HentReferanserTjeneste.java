package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårPeriodeResultatDto;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;

class HentReferanserTjeneste {

    private final BeregningPerioderGrunnlagRepository grunnlagRepository;
    private final VilkårResultatRepository vilkårResultatRepository;

    HentReferanserTjeneste(BeregningPerioderGrunnlagRepository grunnlagRepository,
                                  VilkårResultatRepository vilkårResultatRepository) {
        this.grunnlagRepository = grunnlagRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }


    /** Henter referanser for skjæringstidspunkter
     *
     * Dersom kreverEksisterendeReferanse er satt til true forventes det at referansen finnes fra før for alle skjæringstidspunkt.
     *
     * Dersom skalLageNyVedLikSomInitiell er satt til true lages det nye referanser dersom den eksisterende referansen er initiell (kopiert fra original behandling)
     *
     *
     * @param behandlingId BehandlingId
     * @param skjæringstidspunkter Skjæringstidspunkter
     * @param kreverEksisterendeReferanse Kreves det at eksisterende referanse finnes
     * @param skalLageNyVedLikSomInitiell Skal det lages ny ved initiell referanse
     * @return Liste med referanser
     */
    List<BgRef> finnReferanseEllerLagNy(Long behandlingId,
                                                Collection<LocalDate> skjæringstidspunkter,
                                                boolean kreverEksisterendeReferanse,
                                                boolean skalLageNyVedLikSomInitiell) {
        var refs = new ArrayList<>(finnBeregningsgrunnlagsReferanseFor(behandlingId, skjæringstidspunkter, kreverEksisterendeReferanse, skalLageNyVedLikSomInitiell));

        // generer refs som ikke eksisterer
        var referanserAlleredeDekket = BgRef.getStps(refs);
        for (var stp : skjæringstidspunkter) {
            if (!referanserAlleredeDekket.contains(stp)) {
                refs.add(new BgRef(stp));
            }
        }

        if (refs.size() != skjæringstidspunkter.size()) {
            throw new IllegalStateException("Mismatch størrelse bgReferanser: " + refs + ", skjæringstidspunkter:" + skjæringstidspunkter);
        }

        return Collections.unmodifiableList(refs);
    }


    /** Mapper eksisterende referanser i inneværende behandling mot referanser i originalbehandlingen.
     *
     * Mappes fra referanse til liste fordi originalbehandling han ha flere vilkårsperioder som overlapper med vilkårsperiode fra
     * inneværende behandling.
     *
     *
     * @param ref Behandlingreferanse for denne behandlingen
     * @param vilkårsperioder Liste med aktuelle vilkårsperioder
     * @param bgReferanser Referanser fra denne behandlingen
     * @return Map fra referanse i denne behandlingen til liste av referanser i original behandling
     */
    Map<UUID, List<UUID>> finnMapTilOriginaleReferanserUtenAvslag(BehandlingReferanse ref,
                                                                    Collection<DatoIntervallEntitet> vilkårsperioder,
                                                                    List<BgRef> bgReferanser) {

        return ref.getOriginalBehandlingId()
            .map(id -> {
                Optional<BeregningsgrunnlagPerioderGrunnlag> originaltGrunnlag = grunnlagRepository.hentGrunnlag(id);
                var vilkåreneOpt = vilkårResultatRepository.hentHvisEksisterer(id);
                return finnOrginalReferanserForAllePerioder(vilkårsperioder, bgReferanser, originaltGrunnlag, vilkåreneOpt);
            })
            .orElse(Collections.emptyMap());
    }

    private Map<UUID, List<UUID>> finnOrginalReferanserForAllePerioder(Collection<DatoIntervallEntitet> vilkårsperioder,
                                                                         List<BgRef> bgReferanser,
                                                                         Optional<BeregningsgrunnlagPerioderGrunnlag> originaltGrunnlag,
                                                                         Optional<Vilkårene> originalVilkår) {
        return vilkårsperioder.stream()
            .collect(Collectors.toMap(
            p -> finnReferanseFraPeriode(bgReferanser, p).getRef(),
            p  -> finnReferanserUtenAvslagSomOverlapperPeriode(p, originaltGrunnlag, originalVilkår)));
    }

    private List<UUID> finnReferanserUtenAvslagSomOverlapperPeriode(DatoIntervallEntitet vilkårsperiode,
                                                                    Optional<BeregningsgrunnlagPerioderGrunnlag> originaltGrunnlag,
                                                                    Optional<Vilkårene> originaleVilkår) {
        return originaltGrunnlag
            .stream().flatMap(gr -> gr.getGrunnlagPerioder().stream())
            .filter(periode -> vilkårsperiode.inkluderer(periode.getSkjæringstidspunkt()))
            .filter(periode -> harKunOppfylteVilkår(originaleVilkår, periode))
            .map(BeregningsgrunnlagPeriode::getEksternReferanse)
            .toList();
    }

    private boolean harKunOppfylteVilkår(Optional<Vilkårene> originalVilkår, BeregningsgrunnlagPeriode periode) {
        return originalVilkår.stream().flatMap(v -> v.getVilkårene().stream())
            .flatMap(v -> v.getPerioder().stream())
            .filter(r -> r.getPeriode().getFomDato().equals(periode.getSkjæringstidspunkt()))
            .allMatch(vr -> vr.getUtfall().equals(Utfall.OPPFYLT));
    }

    private BgRef finnReferanseFraPeriode(List<BgRef> bgReferanser, DatoIntervallEntitet p) {
        return bgReferanser.stream().filter(bgRef -> bgRef.getStp().equals(p.getFomDato())).findFirst().orElseThrow();
    }

    List<BgRef> finnBeregningsgrunnlagsReferanseFor(Long behandlingId,
                                                    Collection<LocalDate> skjæringstidspunkter,
                                                    boolean kreverEksisterendeReferanse,
                                                    boolean skalLageNyVedLikSomInitiell) {
        var grunnlagOptional = grunnlagRepository.hentGrunnlag(behandlingId);
        if (grunnlagOptional.isEmpty()) {
            return List.of();
        }

        var bgReferanser = finnBeregningsgrunnlagsReferanseForGrunnlag(behandlingId, grunnlagOptional.get(), skjæringstidspunkter, skalLageNyVedLikSomInitiell);

        if (bgReferanser.isEmpty() && !skjæringstidspunkter.isEmpty()) {
            throw new IllegalStateException("Forventer at referansen eksisterer for skjæringstidspunkt=" + skjæringstidspunkter);
        } else if (kreverEksisterendeReferanse) {
            var first = bgReferanser.stream().filter(BgRef::erGenerertReferanse).findFirst();
            if (first.isPresent()) {
                throw new IllegalStateException("Forventer at referansen eksisterer for skjæringstidspunkt=" + first.get().getStp());
            }
        }
        return bgReferanser;
    }

    private List<BgRef> finnBeregningsgrunnlagsReferanseForGrunnlag(Long behandlingId,
                                                                    BeregningsgrunnlagPerioderGrunnlag grunnlag,
                                                                    Collection<LocalDate> skjæringstidspunkter,
                                                                    boolean skalLageNyVedLikSomInitiell) {

        var grunnlagInitiellVersjon = grunnlagRepository.getInitiellVersjon(behandlingId);
        var resultater = new TreeSet<BgRef>();

        for (var stp : new TreeSet<>(skjæringstidspunkter)) {
            var beregningsgrunnlagPeriodeOpt = grunnlag.finnGrunnlagFor(stp);
            var grunnlagReferanse = beregningsgrunnlagPeriodeOpt.map(BeregningsgrunnlagPeriode::getEksternReferanse);
            if (grunnlagReferanse.isPresent() && skalLageNyVedLikSomInitiell) {
                if (grunnlagInitiellVersjon.isPresent()) {
                    var initReferanse = grunnlagInitiellVersjon.get().finnGrunnlagFor(stp).map(BeregningsgrunnlagPeriode::getEksternReferanse);
                    if (initReferanse.isPresent() && grunnlagReferanse.get().equals(initReferanse.get())) {
                        grunnlagReferanse = Optional.empty();
                    }
                }
            }

            resultater.add(new BgRef(grunnlagReferanse.orElse(null), stp));
        }
        return List.copyOf(resultater);
    }


}
