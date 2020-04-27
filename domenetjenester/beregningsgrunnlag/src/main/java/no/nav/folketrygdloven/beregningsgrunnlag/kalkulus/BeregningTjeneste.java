package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Optional;
import java.util.UUID;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.behandling.BehandlingReferanse;

/**
 * BeregningTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt (https://github.com/navikt/ft-kalkulus/)
 */
public interface BeregningTjeneste {

    /** Starter en ny beregning eller starter en beregning på nytt fra starten av
     * Steg 1. FASTSETT_STP_BER
     * @param referanse {@link BehandlingReferanse}
     * @param ytelseGrunnlag - ytelsespesifikt grunnlag
     * @return KalkulusResultat {@link KalkulusResultat}
     */
    KalkulusResultat startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag);

    /** Kjører en beregning videre fra gitt steg <br>
     * Steg 2. KOFAKBER (Kontroller fakta for beregning)<br>
     * Steg 3. FORS_BERGRUNN (Foreslå beregningsgrunnlag)<br>
     * Steg 4. FORDEL_BERGRUNN (Fordel beregningsgrunnlag)<br>
     * Steg 5. FAST_BERGRUNN (Fastsett beregningsgrunnlag)
     *
     * @param referanse {@link BehandlingReferanse}
     * @param stegType {@link BehandlingStegType}
     * @return KalkulusResultat {@link KalkulusResultat}
     */
    KalkulusResultat fortsettBeregning(BehandlingReferanse referanse, BehandlingStegType stegType);

    /**
     * @param håndterBeregningDto Dto for håndtering av beregning aksjonspunkt
     * @param referanse Behandlingreferanse
     * @return OppdaterBeregningResultat {@link OppdaterBeregningsgrunnlagResultat}
     */
    OppdaterBeregningsgrunnlagResultat oppdaterBeregning(HåndterBeregningDto håndterBeregningDto, BehandlingReferanse referanse);

    Beregningsgrunnlag hentEksaktFastsatt(Long behandlingId);

    BeregningsgrunnlagDto hentBeregningsgrunnlagDto(Long behandlingId);

    Optional<Beregningsgrunnlag> hentFastsatt(Long behandlingId);

    Optional<BeregningsgrunnlagGrunnlag> hentGrunnlag(Long behandlingId);

    void lagreBeregningsgrunnlag(Long id, Beregningsgrunnlag beregningsgrunnlag, BeregningsgrunnlagTilstand opprettet);

    Optional<Beregningsgrunnlag> hentBeregningsgrunnlagForId(UUID uuid, Long behandlingId);

    void deaktiverBeregningsgrunnlag(Long behandlingId);

    Boolean erEndringIBeregning(Long behandlingId1, Long behandlingId2);

}
