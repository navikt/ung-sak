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
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.GrunnbeløpReguleringStatus;
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
     * Kjører en beregning fra gitt steg <br>
     * Steg 1. FASTSETT_STP_NER (Fastsett skjæringstidspunkt beregning)<br>
     * Steg 2. KOFAKBER (Kontroller fakta for beregning)<br>
     * Steg 3. FORS_BERGRUNN (Foreslå beregningsgrunnlag)<br>
     * Steg 4. VURDER_VILKAR_BERGRUNN (Vurder vilkår)<br>
     * Steg 5. VURDER_REF_BERGRUNN (Vurder refusjon)<br>
     * Steg 6. FORDEL_BERGRUNN (Fordel beregningsgrunnlag)<br>
     * Steg 7. FAST_BERGRUNN (Fastsett beregningsgrunnlag)
     *
     * @param referanse behandlingreferanse
     * @param beregningInput input vedrørende vilkårsperiode, referanse og koblinger mot andre original behandling
     * @param stegType {@link BehandlingStegType}
     * @return KalkulusResultat {@link KalkulusResultat}
     */
    SamletKalkulusResultat beregn(BehandlingReferanse referanse,
                                  List<BeregnInput> beregningInput,
                                  BehandlingStegType stegType);

    /** Lager en kopi av grunnlaget lagret i Steg 4: VURDER_VILKAR_BERGRUNN (Vurder vilkår)
     *
     * @param referanse BehandlingReferanse
     * @param beregningInput Input med informasjon om referanser og original referanser
     */
    void kopier(BehandlingReferanse referanse,
                List<BeregnInput> beregningInput);


    /**
     * @param behandlingReferanse Behandlingreferanse
     * @param bgReferanser Liste med referanser og skjæringstidspunkt
     * @param håndterMap Map med dto for håndtering av beregning aksjonspunkt
     * @return Liste av OppdaterBeregningResultat {@link OppdaterBeregningsgrunnlagResultat}
     */
    List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningListe(BehandlingReferanse behandlingReferanse, Collection<BgRef> bgReferanser, Map<UUID, HåndterBeregningDto> håndterMap);

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

    Map<UUID, GrunnbeløpReguleringStatus> kontrollerBehovForGregulering(List<UUID> koblingerÅSpørreMot, Saksnummer saksnummer);

}
