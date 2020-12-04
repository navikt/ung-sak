package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Grunnbeløp;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
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
 * KalkulusApiTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt
 * (https://github.com/navikt/ft-kalkulus/)
 */
public interface KalkulusApiTjeneste {

    /**
     * Starter en ny beregning eller starter en beregning på nytt fra starten av
     * Steg 1. FASTSETT_STP_BER
     *
     * @param referanse {@link BehandlingReferanse}
     * @param startBeregningInput - ytelsespesifikt grunnlag
     * @return KalkulusResultat {@link KalkulusResultat}
     */
    SamletKalkulusResultat startBeregning(BehandlingReferanse referanse, List<StartBeregningInput> startBeregningInput);

    /**
     * Kjører en beregning videre fra gitt steg <br>
     * Steg 2. KOFAKBER (Kontroller fakta for beregning)<br>
     * Steg 3. FORS_BERGRUNN (Foreslå beregningsgrunnlag)<br>
     * Steg 4. VURDER_REF_BERGRUNN (Vurder vilkår og refusjon)<br>
     * Steg 5. FORDEL_BERGRUNN (Fordel beregningsgrunnlag)<br>
     * Steg 6. FAST_BERGRUNN (Fastsett beregningsgrunnlag)
     *
     * @param behandlingReferanse
     * @param bgReferanser per skjæringstidspunkt
     * @param stegType {@link BehandlingStegType}
     * @return KalkulusResultat {@link KalkulusResultat}
     */
    SamletKalkulusResultat fortsettBeregning(BehandlingReferanse behandlingReferanse, Collection<BgRef> bgReferanser, BehandlingStegType stegType);

    /**
     * @param behandlingReferanse Behandlingreferanse
     * @param håndterMap Map med dto for håndtering av beregning aksjonspunkt
     * @return Liste av OppdaterBeregningResultat {@link OppdaterBeregningsgrunnlagResultat}
     */
    List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningListe(BehandlingReferanse behandlingReferanse, Map<UUID, HåndterBeregningDto> håndterMap);

    List<Beregningsgrunnlag> hentEksaktFastsatt(BehandlingReferanse ref, Collection<BgRef> bgReferanse);

    BeregningsgrunnlagListe hentBeregningsgrunnlagListeDto(BehandlingReferanse referanse, Set<BeregningsgrunnlagReferanse> bgReferanser);

    List<BeregningsgrunnlagGrunnlag> hentGrunnlag(BehandlingReferanse ref, Collection<BgRef> bgReferanser);

    void lagreBeregningsgrunnlag(BehandlingReferanse referanse, Beregningsgrunnlag beregningsgrunnlag, BeregningsgrunnlagTilstand opprettet);

    /**
     * Deaktiverer beregningsgrunnlag og kalkulatorinput.
     *
     * Skal kun kalles fra første beregningssteg.
     *
     * @param fagsakYtelseType Fagsakytelsetype
     * @param bgReferanse koblingreferanse
     */
    void deaktiverBeregningsgrunnlag(FagsakYtelseType fagsakYtelseType, Saksnummer saksnummer, List<UUID> bgReferanse);

    /**
     * Henter grunnbeløp (GVERDI)
     *
     * @param dato Aktuell dato for grunnbeløp
     * @return Sats-objekt med verdi og periode for grunnbeløp
     */
    Grunnbeløp hentGrunnbeløp(LocalDate dato);

}
