package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;

/**
 * BeregningTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt (https://github.com/navikt/ft-kalkulus/)
 */
public interface BeregningTjeneste {

    /**
     * Starter en ny beregning eller starter en beregning på nytt fra starten av
     * Steg 1. FASTSETT_STP_BER
     *
     * @param referanse {@link BehandlingReferanse}
     * @param vilkårsperioder - alle perioder til vurdering
     * @return SamletKalkulusResultat {@link SamletKalkulusResultat}
     */
    SamletKalkulusResultat startBeregning(BehandlingReferanse referanse, Collection<PeriodeTilVurdering> vilkårsperioder, BehandlingStegType stegType);

    /**
     * Kjører en beregning videre fra gitt steg <br>
     * Steg 2. KOFAKBER (Kontroller fakta for beregning)<br>
     * Steg 3. FORS_BERGRUNN (Foreslå beregningsgrunnlag)<br>
     * Steg 4. VURDER_VILKAR_BERGRUNN (Vurder vilkår)<br>
     * Steg 5. VURDER_REF_BERGRUNN (Vurder refusjon)<br>
     * Steg 6. FORDEL_BERGRUNN (Fordel beregningsgrunnlag)<br>
     * Steg 7. FAST_BERGRUNN (Fastsett beregningsgrunnlag)
     *
     * @param referanse {@link BehandlingReferanse}
     * @param vilkårsperioder Vilkårsperioder til vurdering
     * @param stegType {@link BehandlingStegType}
     * @return SamletKalkulusResultat {@link KalkulusResultat}
     */
    SamletKalkulusResultat beregn(BehandlingReferanse referanse, Collection<PeriodeTilVurdering> vilkårsperioder, BehandlingStegType stegType);

    /** Kopierer beregningsgrunnlaget lagret i steg VURDER_VILKAR_BERGRUNN (Vurder vilkår) fra original behandling for hver vilkårsperiode
     * @param referanse Behandlingreferanse
     * @param vilkårsperioder Vilkårsperioder
     */
    void kopier(BehandlingReferanse referanse, Collection<PeriodeTilVurdering> vilkårsperioder);


    /**
     * @param håndterBeregningDto Dto for håndtering av beregning aksjonspunkt
     * @param ref {@link BehandlingReferanse}
     * @param skjæringstidspunkt - skjæringtidspunkt
     * @return OppdaterBeregningResultat {@link OppdaterBeregningsgrunnlagResultat}
     */
    OppdaterBeregningsgrunnlagResultat oppdaterBeregning(HåndterBeregningDto håndterBeregningDto, BehandlingReferanse ref, LocalDate skjæringstidspunkt);

    /**
     * @param håndterMap Map fra skjæringstidspunkt til Dto for håndtering av beregning aksjonspunkt
     * @param ref {@link BehandlingReferanse}
     * @return Liste av OppdaterBeregningResultat {@link OppdaterBeregningsgrunnlagResultat}
     */
    List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningListe(Map<LocalDate, HåndterBeregningDto> håndterMap, BehandlingReferanse ref);

    List<Beregningsgrunnlag> hentEksaktFastsatt(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter);

    List<Beregningsgrunnlag> hentEksaktFastsattForAllePerioderInkludertAvslag(BehandlingReferanse ref);

    List<Beregningsgrunnlag> hentEksaktFastsattForAllePerioder(BehandlingReferanse ref);

    List<BeregningsgrunnlagDto> hentBeregningsgrunnlagDtoer(BehandlingReferanse ref);

    List<BeregningsgrunnlagGrunnlag> hentGrunnlag(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter);

    List<BeregningsgrunnlagKobling> hentKoblingerForInnvilgedePerioder(BehandlingReferanse ref);

    List<BeregningsgrunnlagKobling> hentKoblingerForInnvilgedePerioderTilVurdering(BehandlingReferanse ref);


    /** Deaktiverer beregningsgrunnlaget og tilhørende input. Fører til at man ikke har noen aktive beregningsgrunnlag.
     *
     * Deaktivering skal kun kalles i første steg i beregning.
     *  @param ref Behandlingreferanse
     *
     */
    public void deaktiverBeregningsgrunnlagUtenTilknytningTilVilkår(BehandlingReferanse ref);

    /** Gjenoppretter det første beregningsgrunnlaget som var opprettet for behandlingen
     *
     * Brukes kun av FRISINN
     *
     * @param ref Behandlingreferanse
     */
    void gjenopprettInitiell(BehandlingReferanse ref);

    /** Samlet beregningsgrunnlag for visning i GUI bla. */
    Optional<BeregningsgrunnlagListe> hentBeregningsgrunnlag(BehandlingReferanse ref);

}
