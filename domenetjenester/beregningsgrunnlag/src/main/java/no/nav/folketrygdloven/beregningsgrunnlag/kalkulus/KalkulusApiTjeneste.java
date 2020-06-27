package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
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
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.behandling.BehandlingReferanse;

/**
 * KalkulusApiTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt (https://github.com/navikt/ft-kalkulus/)
 */
public interface KalkulusApiTjeneste {

    /** Starter en ny beregning eller starter en beregning på nytt fra starten av
     * Steg 1. FASTSETT_STP_BER
     * @param referanse {@link BehandlingReferanse}
     * @param ytelseGrunnlag - ytelsespesifikt grunnlag
     * @param bgReferanse
     * @param skjæringstidspunkt
     * @return KalkulusResultat {@link KalkulusResultat}
     */
    KalkulusResultat startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, UUID bgReferanse, LocalDate skjæringstidspunkt);

    /** Kjører en beregning videre fra gitt steg <br>
     * Steg 2. KOFAKBER (Kontroller fakta for beregning)<br>
     * Steg 3. FORS_BERGRUNN (Foreslå beregningsgrunnlag)<br>
     * Steg 4. FORDEL_BERGRUNN (Fordel beregningsgrunnlag)<br>
     * Steg 5. FAST_BERGRUNN (Fastsett beregningsgrunnlag)
     *
     * @param fagsakYtelseType
     * @param behandlingUuid
     * @param stegType {@link BehandlingStegType}
     * @return KalkulusResultat {@link KalkulusResultat}
     */
    KalkulusResultat fortsettBeregning(FagsakYtelseType fagsakYtelseType, UUID behandlingUuid, BehandlingStegType stegType);

    /**
     * @param håndterBeregningDto Dto for håndtering av beregning aksjonspunkt
     * @param behandlingUuid
     * @return OppdaterBeregningResultat {@link OppdaterBeregningsgrunnlagResultat}
     */
    OppdaterBeregningsgrunnlagResultat oppdaterBeregning(HåndterBeregningDto håndterBeregningDto, UUID behandlingUuid);

    Optional<Beregningsgrunnlag> hentEksaktFastsatt(FagsakYtelseType fagsakYtelseType, UUID bgReferanse);

    BeregningsgrunnlagDto hentBeregningsgrunnlagDto(BehandlingReferanse referanse, UUID bgReferanse, LocalDate skjæringstidspunkt);

    Optional<Beregningsgrunnlag> hentFastsatt(UUID bgReferanse, FagsakYtelseType fagsakYtelseType);

    Optional<BeregningsgrunnlagGrunnlag> hentGrunnlag(FagsakYtelseType fagsakYtelseType, UUID uuid);

    void lagreBeregningsgrunnlag(BehandlingReferanse referanse, Beregningsgrunnlag beregningsgrunnlag, BeregningsgrunnlagTilstand opprettet);

    Optional<Beregningsgrunnlag> hentBeregningsgrunnlagForId(UUID bgReferanse, FagsakYtelseType fagsakYtelseType, UUID uuid);

    /** Deaktiverer beregningsgrunnlag og kalkulatorinput.
     *
     * Skal kun kalles fra første beregningssteg.
     *
     * @param fagsakYtelseType Fagsakytelsetype
     * @param bgReferanse koblingreferanse
     */
    void deaktiverBeregningsgrunnlag(FagsakYtelseType fagsakYtelseType, UUID bgReferanse);

    Boolean erEndringIBeregning(FagsakYtelseType fagsakYtelseType1, UUID bgRefeanse1, FagsakYtelseType fagsakYtelseType2, UUID bgReferanse2);

}
