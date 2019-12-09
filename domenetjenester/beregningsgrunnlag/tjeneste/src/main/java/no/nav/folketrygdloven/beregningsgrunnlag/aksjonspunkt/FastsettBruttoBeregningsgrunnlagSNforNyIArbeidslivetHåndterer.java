package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import no.nav.folketrygdloven.beregningsgrunnlag.Kopimaskin;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;

@ApplicationScoped
public class FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer() {
        // for CDI proxy
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public void oppdater(Long behandlingId, FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto) {
        Integer bruttoBeregningsgrunnlag = dto.getBruttoBeregningsgrunnlag();
        if (bruttoBeregningsgrunnlag != null) {
            BeregningsgrunnlagEntitet grunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId);
            BeregningsgrunnlagEntitet nyttGrunnlag = Kopimaskin.deepCopy(grunnlag);
            nyttGrunnlag.getBeregningsgrunnlagPerioder().forEach(bgPeriode -> {
                BeregningsgrunnlagPrStatusOgAndel bgAndel = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                    .filter(bpsa -> bpsa.getAktivitetStatus().erSelvstendigNæringsdrivende())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Mangler andel for selvstendig næringsdrivende (eller kombinasjon med SN) for behandling "  + behandlingId));

                BeregningsgrunnlagPrStatusOgAndel.builder(bgAndel)
                    .medOverstyrtPrÅr(BigDecimal.valueOf(bruttoBeregningsgrunnlag))
                    .build(bgPeriode);
            });
            beregningsgrunnlagRepository.lagre(behandlingId, nyttGrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        }
    }
}
