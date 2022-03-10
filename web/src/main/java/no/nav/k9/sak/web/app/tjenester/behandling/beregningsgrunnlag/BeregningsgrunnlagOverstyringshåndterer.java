package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeløpEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPeriodeEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.InntektskategoriEndring;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.OverstyrBeregningsgrunnlagDto;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning.BeregningsgrunnlagVerdierHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsgrunnlagDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsgrunnlagOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsgrunnlagDto> {

    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;
    private BeregningsgrunnlagVerdierHistorikkTjeneste verdierHistorikkTjeneste;

    BeregningsgrunnlagOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsgrunnlagOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                   BeregningTjeneste kalkulusTjeneste,
                                                   BeregningsgrunnlagVilkårTjeneste vilkårTjeneste, BeregningsgrunnlagVerdierHistorikkTjeneste verdierHistorikkTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG);
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.verdierHistorikkTjeneste = verdierHistorikkTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsgrunnlagDto dto,
                                                  Behandling behandling, BehandlingskontrollKontekst kontekst) {
        HåndterBeregningDto håndterBeregningDto = MapDtoTilRequest.mapOverstyring(dto);
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        validerOppdatering(dto.getPeriode().getFom(), behandlingReferanse);
        var oppdaterBeregningsgrunnlagResultat = kalkulusTjeneste.oppdaterBeregning(håndterBeregningDto, behandlingReferanse, dto.getPeriode().getFom());
        oppdaterBeregningsgrunnlagResultat.getBeregningsgrunnlagEndring().ifPresent(
            endring -> verdierHistorikkTjeneste.lagHistorikkForBeregningsgrunnlagVerdier(behandling.getId(),
            endring.getBeregningsgrunnlagPeriodeEndringer().get(0), getHistorikkAdapter().tekstBuilder()));
        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        fjernOverstyrtAksjonspunkt(behandling)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    private Optional<Aksjonspunkt> fjernOverstyrtAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsgrunnlagDto dto) {
        // Håndteres ved oppdatering for å kunne bruke endringsobjekt som returneres fra kalkulus
    }

    private void validerOppdatering(LocalDate stp,
                                    BehandlingReferanse ref) {
        NavigableSet<DatoIntervallEntitet> perioderSomSkalKunneVurderes = vilkårTjeneste.utledPerioderTilVurdering(ref, false);
        var erTilVurdering = perioderSomSkalKunneVurderes.stream().anyMatch(p -> p.getFomDato().equals(stp));
        if (!erTilVurdering) {
            throw new IllegalStateException("Prøver å endre grunnlag med skjæringstidspunkt" + stp + " men denne er ikke i" +
                " listen over vilkårsperioder som er til vurdering " + perioderSomSkalKunneVurderes);
        }
    }

}
