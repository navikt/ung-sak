package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.LagFortsettRequest;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.FortsettBeregningListeRequest;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("PSB")
@BehandlingStegRef(kode = "VURDER_REF_BERGRUNN")
@ApplicationScoped
class PSBVurderRefusjonLagFortsettRequest implements LagFortsettRequest {

    private PSBYtelsesspesifiktGrunnlagMapper ytelsesspesifiktGrunnlagMapper;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;

    public PSBVurderRefusjonLagFortsettRequest() {
    }

    @Inject
    public PSBVurderRefusjonLagFortsettRequest(PSBYtelsesspesifiktGrunnlagMapper ytelsesspesifiktGrunnlagMapper, BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste) {
        this.ytelsesspesifiktGrunnlagMapper = ytelsesspesifiktGrunnlagMapper;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
    }

    @Override
    public FortsettBeregningListeRequest lagRequest(BehandlingReferanse referanse, Collection<BgRef> bgReferanser, BehandlingStegType stegType) {
        var bgRefs = BgRef.getRefs(bgReferanser);
        var stpMap = bgReferanser.stream().collect(Collectors.toMap(BgRef::getRef, BgRef::getStp));
        var ytelseType = YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode());
        var ytelsesspesifiktGrunnlagPrReferanse = getYtelsesspesifiktGrunnlagPrReferanse(referanse, stpMap);
        return FortsettBeregningListeRequest.medOppdaterteUtbetalingsgrader(
            referanse.getSaksnummer().getVerdi(),
            bgRefs,
            ytelsesspesifiktGrunnlagPrReferanse,
            ytelseType,
            new StegType(stegType.getKode()));
    }

    private Map<UUID, YtelsespesifiktGrunnlagDto> getYtelsesspesifiktGrunnlagPrReferanse(BehandlingReferanse behandlingReferanse,
                                                                                         Map<UUID, LocalDate> referanseSkjæringstidspunktMap) {
        var utledPerioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(behandlingReferanse, false);

        return referanseSkjæringstidspunktMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .map(entry -> {
                UUID bgReferanse = entry.getKey();
                var vilkårPeriode = finnPeriodeForSkjæringstidspunkt(utledPerioderTilVurdering, entry);
                var ytelsesGrunnlag = ytelsesspesifiktGrunnlagMapper.lagYtelsespesifiktGrunnlag(behandlingReferanse, vilkårPeriode);
                return new AbstractMap.SimpleEntry<>(bgReferanse, ytelsesGrunnlag);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private DatoIntervallEntitet finnPeriodeForSkjæringstidspunkt(NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering, Map.Entry<UUID, LocalDate> entry) {
        return utledPerioderTilVurdering.stream()
            .filter(p -> p.getFomDato().equals(entry.getValue()))
            .findFirst().orElseThrow(() -> new IllegalStateException("Fant ikke periode for skjæringstidspunkt"));
    }


}
