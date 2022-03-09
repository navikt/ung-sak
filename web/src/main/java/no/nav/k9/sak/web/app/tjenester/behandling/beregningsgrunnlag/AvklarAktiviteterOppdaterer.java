package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.AvklarteAktiviteterDtoer;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BekreftetBeregningsgrunnlagDto;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning.BeregningsaktivitetHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarteAktiviteterDtoer.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAktiviteterOppdaterer implements AksjonspunktOppdaterer<AvklarteAktiviteterDtoer> {

    private BeregningsgrunnlagOppdateringTjeneste oppdateringjeneste;
    private BeregningsaktivitetHistorikkTjeneste historikkTjeneste;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    AvklarAktiviteterOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAktiviteterOppdaterer(BeregningsgrunnlagOppdateringTjeneste oppdateringjeneste,
                                       BeregningsaktivitetHistorikkTjeneste historikkTjeneste,
                                       BeregningsgrunnlagVilkårTjeneste vilkårTjeneste,
                                       HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.oppdateringjeneste = oppdateringjeneste;
        this.historikkTjeneste = historikkTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarteAktiviteterDtoer dtoer,
                                        AksjonspunktOppdaterParameter param) {
        Map<LocalDate, HåndterBeregningDto> stpTilDtoMap = dtoer.getGrunnlag().stream()
            .collect(Collectors.toMap(dto -> dto.getPeriode().getFom(), dto1 -> MapDtoTilRequest.map(dto1, dtoer.getBegrunnelse())));
        stpTilDtoMap.keySet().forEach(e -> validerOppdatering(e, param.getRef()));
        var oppdaterBeregningsgrunnlagResultat = oppdateringjeneste.oppdaterBeregning(stpTilDtoMap, param.getRef());
        lagHistorikk(dtoer, param.getRef(), oppdaterBeregningsgrunnlagResultat);
        return OppdateringResultat.utenOverhopp();
    }

    private void lagHistorikk(AvklarteAktiviteterDtoer dto,
                              BehandlingReferanse behandlingReferanse,
                              List<OppdaterBeregningsgrunnlagResultat> oppdaterBeregningsgrunnlagResultater) {
        if (oppdaterBeregningsgrunnlagResultater.stream().anyMatch(e -> !e.getBeregningAktivitetEndringer().isEmpty())) {
            var historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
            historikkInnslagTekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING);
            oppdaterBeregningsgrunnlagResultater.forEach(resultat -> lagOgFerdigstillHistorikkdelForPeriode(dto, behandlingReferanse, historikkInnslagTekstBuilder, resultat));
            historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingReferanse.getId(), HistorikkinnslagType.FAKTA_ENDRET);
        }
    }

    private void lagOgFerdigstillHistorikkdelForPeriode(AvklarteAktiviteterDtoer dto, BehandlingReferanse behandlingReferanse, HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, OppdaterBeregningsgrunnlagResultat resultat) {
        historikkTjeneste.lagHistorikkForSkjæringstidspunkt(behandlingReferanse.getId(),
            historikkInnslagTekstBuilder,
            resultat.getBeregningAktivitetEndringer(),
            resultat.getSkjæringstidspunkt(),
            finnBegrunnelse(dto, resultat.getSkjæringstidspunkt()));
        historikkInnslagTekstBuilder.ferdigstillHistorikkinnslagDel();
    }

    private String finnBegrunnelse(AvklarteAktiviteterDtoer dto, LocalDate skjæringstidspunkt) {
        return dto.getGrunnlag().stream().filter(g -> g.getPeriode().getFom().equals(skjæringstidspunkt))
            .findFirst()
            .map(BekreftetBeregningsgrunnlagDto::getBegrunnelse).orElse(dto.getBegrunnelse());
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
