package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;
import java.math.BigDecimal;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.Kopimaskin;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.iay.AktivitetStatus;

@ApplicationScoped
public class VurderVarigEndretNyoppstartetSNHåndterer {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    VurderVarigEndretNyoppstartetSNHåndterer() {
        // for CDI proxy
    }

    @Inject
    public VurderVarigEndretNyoppstartetSNHåndterer(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public void håndter(Long behandlingId, VurderVarigEndringEllerNyoppstartetSNDto dto) {
        Integer bruttoBeregningsgrunnlag = dto.getBruttoBeregningsgrunnlag();
        if (bruttoBeregningsgrunnlag != null) {
            BeregningsgrunnlagEntitet grunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId);
            BeregningsgrunnlagEntitet nyttGrunnlag = Kopimaskin.deepCopy(grunnlag);
            List<BeregningsgrunnlagPeriode> bgPerioder = nyttGrunnlag.getBeregningsgrunnlagPerioder();
            for (BeregningsgrunnlagPeriode bgPeriode : bgPerioder) {
                BeregningsgrunnlagPrStatusOgAndel bgAndel = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                    .filter(bpsa -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE.equals(bpsa.getAktivitetStatus()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Mangler BeregningsgrunnlagPrStatusOgAndel[SELVSTENDIG_NÆRINGSDRIVENDE] for behandling " + behandlingId));

                BeregningsgrunnlagPrStatusOgAndel.builder(bgAndel)
                    .medOverstyrtPrÅr(BigDecimal.valueOf(bruttoBeregningsgrunnlag))
                    .build(bgPeriode);
            }
            beregningsgrunnlagRepository.lagre(behandlingId, nyttGrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        }
    }

}