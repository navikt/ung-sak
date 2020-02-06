package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.MatchBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsettBeregningsgrunnlagAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.OverstyrBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.RedigerbarAndelFaktaOmBeregningDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.VurderFaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller.FaktaOmBeregningTilfellerOppdaterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;

@ApplicationScoped
public class BeregningFaktaOgOverstyringHåndterer {

    private FaktaOmBeregningTilfellerOppdaterer faktaOmBeregningTilfellerOppdaterer;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    public BeregningFaktaOgOverstyringHåndterer() {
        // For CDI
    }

    @Inject
    public BeregningFaktaOgOverstyringHåndterer(FaktaOmBeregningTilfellerOppdaterer faktaOmBeregningTilfellerOppdaterer, BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.faktaOmBeregningTilfellerOppdaterer = faktaOmBeregningTilfellerOppdaterer;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public void håndter(BehandlingReferanse behandlingReferanse, VurderFaktaOmBeregningDto faktaDto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId());
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = beregningsgrunnlag.dypKopi();
        Optional<BeregningsgrunnlagEntitet> forrigeBg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingReferanse.getId(), behandlingReferanse.getOriginalBehandlingId(), BeregningsgrunnlagTilstand.KOFAKBER_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        faktaOmBeregningTilfellerOppdaterer.oppdater(faktaDto.getFakta(), behandlingReferanse, nyttBeregningsgrunnlag, forrigeBg);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), nyttBeregningsgrunnlag, BeregningsgrunnlagTilstand.KOFAKBER_UT);
    }


    public void håndterMedOverstyring(BehandlingReferanse behandlingReferanse, OverstyrBeregningsgrunnlagDto dto) {
        // Overstyring kan kun gjøres på grunnlaget fra 98-steget
        BeregningsgrunnlagEntitet bgOppdatertMedAndeler = beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .orElseThrow(() -> new IllegalStateException("Kan ikke overstyre uten et opprettet beregningsgrunnlag"));
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = bgOppdatertMedAndeler.dypKopi();
        Optional<BeregningsgrunnlagEntitet> forrigeBg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingReferanse.getId(), behandlingReferanse.getOriginalBehandlingId(), BeregningsgrunnlagTilstand.KOFAKBER_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        FaktaBeregningLagreDto fakta = dto.getFakta();
        if (fakta != null) {
            faktaOmBeregningTilfellerOppdaterer.oppdater(fakta, behandlingReferanse, nyttBeregningsgrunnlag, forrigeBg);
        }
        overstyrInntekterPrPeriode(nyttBeregningsgrunnlag, forrigeBg, dto.getOverstyrteAndeler());
        BeregningsgrunnlagEntitet overstyrtGrunnlag = BeregningsgrunnlagEntitet.builder(nyttBeregningsgrunnlag).medOverstyring(true).build();
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), overstyrtGrunnlag, BeregningsgrunnlagTilstand.KOFAKBER_UT);
    }

    private void overstyrInntekterPrPeriode(BeregningsgrunnlagEntitet nyttGrunnlag,
                                                         Optional<BeregningsgrunnlagEntitet> forrigeBg,
                                                         List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler) {
        List<BeregningsgrunnlagPeriode> bgPerioder = nyttGrunnlag.getBeregningsgrunnlagPerioder();
        for (BeregningsgrunnlagPeriode bgPeriode : bgPerioder) {
            Optional<BeregningsgrunnlagPeriode> forrigeBgPeriode = MatchBeregningsgrunnlagTjeneste
                .finnOverlappendePeriodeOmKunEnFinnes(bgPeriode, forrigeBg);
            overstyrteAndeler
                .forEach(andelDto ->
                    FastsettBeregningVerdierTjeneste.fastsettVerdierForAndel(mapTilRedigerbarAndelDto(andelDto), andelDto.getFastsatteVerdier(), bgPeriode, forrigeBgPeriode));
        }
    }

    private RedigerbarAndelFaktaOmBeregningDto mapTilRedigerbarAndelDto(FastsettBeregningsgrunnlagAndelDto andelDto) {
        return new RedigerbarAndelFaktaOmBeregningDto(false, andelDto.getAndelsnr(), andelDto.getLagtTilAvSaksbehandler());
    }

}
