package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;

/**
 * BeregningTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt (https://github.com/navikt/ft-kalkulus/)
 */
public interface BeregningTjeneste {

    /** Starter en ny beregning eller starter en beregning på nytt fra starten av
     * Steg 1. FASTSETT_STP_BER
     * @param referanse {@link BehandlingReferanse}
     * @param ytelseGrunnlag - ytelsespesifikt grunnlag
     * @param skjæringstidspunkt - key for å finne bgReferanse mot kalkulus
     * @return KalkulusResultat {@link KalkulusResultat}
     */
    KalkulusResultat startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, LocalDate skjæringstidspunkt);

    /** Kjører en beregning videre fra gitt steg <br>
     * Steg 2. KOFAKBER (Kontroller fakta for beregning)<br>
     * Steg 3. FORS_BERGRUNN (Foreslå beregningsgrunnlag)<br>
     * Steg 4. FORDEL_BERGRUNN (Fordel beregningsgrunnlag)<br>
     * Steg 5. FAST_BERGRUNN (Fastsett beregningsgrunnlag)
     *
     * @param ref {@link BehandlingReferanse}
     * @param skjæringstidspunkt - skjæringstidspunktet
     * @param stegType {@link BehandlingStegType}
     * @return KalkulusResultat {@link KalkulusResultat}
     */
    KalkulusResultat fortsettBeregning(BehandlingReferanse ref, LocalDate skjæringstidspunkt, BehandlingStegType stegType);

    /**
     * @param håndterBeregningDto Dto for håndtering av beregning aksjonspunkt
     * @param ref {@link BehandlingReferanse}
     * @param skjæringstidspunkt - skjæringtidspunkt
     * @return OppdaterBeregningResultat {@link OppdaterBeregningsgrunnlagResultat}
     */
    OppdaterBeregningsgrunnlagResultat oppdaterBeregning(HåndterBeregningDto håndterBeregningDto, BehandlingReferanse ref, LocalDate skjæringstidspunkt);

    Optional<Beregningsgrunnlag> hentEksaktFastsatt(BehandlingReferanse ref, LocalDate skjæringstidspunkt);

    List<Beregningsgrunnlag> hentEksaktFastsattForAllePerioderInkludertAvslag(BehandlingReferanse ref);

    List<Beregningsgrunnlag> hentEksaktFastsattForAllePerioder(BehandlingReferanse ref);

    List<BeregningsgrunnlagDto> hentBeregningsgrunnlagDtoer(BehandlingReferanse ref);

    Optional<Beregningsgrunnlag> hentFastsatt(BehandlingReferanse ref, LocalDate skjæringstidspunkt);

    Optional<BeregningsgrunnlagGrunnlag> hentGrunnlag(BehandlingReferanse ref, LocalDate skjæringstidspunkt);

    List<BeregningsgrunnlagKobling> hentKoblinger(BehandlingReferanse ref);

    /** Deaktiverer beregningsgrunnlaget og tilhørende input. Fører til at man ikke har noen aktive beregningsgrunnlag.
     *
     * Deaktivering skal kun kalles i første steg i beregning.
     *
     * @param ref Behandlingreferanse
     * @param skjæringstidspunkt skjæringstidspunkt
     */
    void deaktiverBeregningsgrunnlag(BehandlingReferanse ref, LocalDate skjæringstidspunkt);

    /** Gjenoppretter det første beregningsgrunnlaget som var opprettet for behandlingen
     *
     * Brukes kun av FRISINN
     *
     * @param ref Behandlingreferanse
     */
    void gjenopprettInitiell(BehandlingReferanse ref);

    Boolean erEndringIBeregning(Long behandlingId1, Long behandlingId2, LocalDate skjæringstidspunkt);
}
