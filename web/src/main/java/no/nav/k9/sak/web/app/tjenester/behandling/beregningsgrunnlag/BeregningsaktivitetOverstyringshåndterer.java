package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;


import java.time.LocalDate;
import java.util.NavigableSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.OverstyrBeregningsaktiviteterDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsaktiviteterDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsaktivitetOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsaktiviteterDto> {

    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;

    BeregningsaktivitetOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsaktivitetOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                    BeregningTjeneste kalkulusTjeneste,
                                                    BeregningsgrunnlagVilkårTjeneste vilkårTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER);
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsaktiviteterDto dto, Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {

        // Sjekker at vi ikke oppaterer grunnlag som ikke er til vurdering
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        validerOppdatering(dto.getPeriode().getFom(), behandlingReferanse);
        HåndterBeregningDto håndterBeregningDto = MapDtoTilRequest.mapOverstyring(dto);
        kalkulusTjeneste.oppdaterBeregning(håndterBeregningDto, behandlingReferanse, dto.getPeriode().getFom());
        return OppdateringResultat.utenOverhopp();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsaktiviteterDto dto) {
        // TODO Fiks historikk
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
