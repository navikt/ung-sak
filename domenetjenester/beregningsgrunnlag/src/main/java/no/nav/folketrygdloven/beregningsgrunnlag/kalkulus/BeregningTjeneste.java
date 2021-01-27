package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * BeregningTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt (https://github.com/navikt/ft-kalkulus/)
 */
public interface BeregningTjeneste {

    /**
     * Starter en ny beregning eller starter en beregning på nytt fra starten av
     * Steg 1. FASTSETT_STP_BER
     *
     * @param referanse {@link BehandlingReferanse}
     * @param ytelseGrunnlag - ytelsespesifikt grunnlag per skjæringstidspunkt
     * @return SamletKalkulusResultat {@link KalkulusResultat}
     */
    SamletKalkulusResultat startBeregning(BehandlingReferanse referanse, List<DatoIntervallEntitet> vilkårsperioder);

    /**
     * Kjører en beregning videre fra gitt steg <br>
     * Steg 2. KOFAKBER (Kontroller fakta for beregning)<br>
     * Steg 3. FORS_BERGRUNN (Foreslå beregningsgrunnlag)<br>
     * Steg 4. VURDER_REF_BERGRUNN (Vurder vilkår og refusjon)<br>
     * Steg 5. FORDEL_BERGRUNN (Fordel beregningsgrunnlag)<br>
     * Steg 6. FAST_BERGRUNN (Fastsett beregningsgrunnlag)
     *
     * @param ref {@link BehandlingReferanse}
     * @param skjæringstidspunkt - skjæringstidspunktet
     * @param stegType {@link BehandlingStegType}
     * @return SamletKalkulusResultat {@link KalkulusResultat}
     */
    SamletKalkulusResultat fortsettBeregning(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter, BehandlingStegType stegType);

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

    List<BeregningsgrunnlagKobling> hentKoblinger(BehandlingReferanse ref);

    /** Deaktiverer beregningsgrunnlaget og tilhørende input. Fører til at man ikke har noen aktive beregningsgrunnlag.
     *
     * Deaktivering skal kun kalles i første steg i beregning.
     *
     * @param ref Behandlingreferanse
     * @param skjæringstidspunkt
     */
    public void deaktiverBeregningsgrunnlag(BehandlingReferanse ref, Collection<LocalDate> skjæringstidspunkter);

    /** Gjenoppretter det første beregningsgrunnlaget som var opprettet for behandlingen
     *
     * Brukes kun av FRISINN
     *
     * @param ref Behandlingreferanse
     */
    void gjenopprettInitiell(BehandlingReferanse ref);

}
