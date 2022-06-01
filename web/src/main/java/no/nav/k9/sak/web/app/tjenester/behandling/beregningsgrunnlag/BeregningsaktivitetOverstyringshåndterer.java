package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;


import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.OverstyrBeregningsaktiviteterDto;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning.BeregningsaktivitetHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsaktiviteterDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsaktivitetOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsaktiviteterDto> {

    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;
    private BeregningsaktivitetHistorikkTjeneste historikkTjeneste;
    private VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;

    BeregningsaktivitetOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsaktivitetOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                    BeregningTjeneste kalkulusTjeneste,
                                                    BeregningsgrunnlagVilkårTjeneste vilkårTjeneste,
                                                    BeregningsaktivitetHistorikkTjeneste historikkTjeneste,
                                                    VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER);
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.historikkTjeneste = historikkTjeneste;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsaktiviteterDto dto, Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {

        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        // Sjekker at vi ikke oppaterer grunnlag som ikke er til vurdering
        validerOppdatering(dto.getPeriode().getFom(), behandlingReferanse);
        HåndterBeregningDto håndterBeregningDto = MapDtoTilRequest.mapOverstyring(dto);
        var oppdaterBeregningsgrunnlagResultat = kalkulusTjeneste.oppdaterBeregning(håndterBeregningDto, behandlingReferanse, dto.getPeriode().getFom());
        lagHistorikk(dto, behandling, oppdaterBeregningsgrunnlagResultat);
        return OppdateringResultat.nyttResultat();
    }

    private void lagHistorikk(OverstyrBeregningsaktiviteterDto dto, Behandling behandling, OppdaterBeregningsgrunnlagResultat oppdaterBeregningsgrunnlagResultat) {
        var tekstBuilder = getHistorikkAdapter().tekstBuilder();
        if (!oppdaterBeregningsgrunnlagResultat.getBeregningAktivitetEndringer().isEmpty()) {
            historikkTjeneste.lagHistorikkForSkjæringstidspunkt(behandling.getId(),
                tekstBuilder,
                oppdaterBeregningsgrunnlagResultat.getBeregningAktivitetEndringer(),
                oppdaterBeregningsgrunnlagResultat.getSkjæringstidspunkt(),
                dto.getBegrunnelse());
        }
        tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING);
        getHistorikkAdapter().opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET); // Lager historikk for fakta siden det er fakta om overstyres
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsaktiviteterDto dto) {
        // Historikk lages ved oppdatering og kall mot kalkulus
    }

    private void validerOppdatering(LocalDate stp,
                                    BehandlingReferanse ref) {
        var filter = vilkårPeriodeFilterProvider.getFilter(ref, false);
        filter.ignorerForlengelseperioder();
        var perioderSomSkalKunneVurderes = vilkårTjeneste.utledPerioderTilVurdering(ref, filter);
        var erTilVurdering = perioderSomSkalKunneVurderes.stream().anyMatch(p -> p.getFomDato().equals(stp));
        if (!erTilVurdering) {
            throw new IllegalStateException("Prøver å endre grunnlag med skjæringstidspunkt" + stp + " men denne er ikke i" +
                " listen over vilkårsperioder som er til vurdering " + perioderSomSkalKunneVurderes);
        }
    }

}
