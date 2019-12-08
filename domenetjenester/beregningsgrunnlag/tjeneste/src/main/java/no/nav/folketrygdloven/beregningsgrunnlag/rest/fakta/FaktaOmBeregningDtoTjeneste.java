package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;

@ApplicationScoped
public class FaktaOmBeregningDtoTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private List<FaktaOmBeregningTilfelleDtoTjeneste> dtoTjenester;
    private AvklarAktiviteterDtoTjeneste avklarAktiviteterDtoTjeneste;
    private AndelerForFaktaOmBeregningTjeneste andelerForFaktaOmBeregningTjeneste;

    FaktaOmBeregningDtoTjeneste() {
        // Hibernate
    }

    @Inject
    public FaktaOmBeregningDtoTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                       @Any Instance<FaktaOmBeregningTilfelleDtoTjeneste> dtoTjenesteInstance,
                                       AvklarAktiviteterDtoTjeneste avklarAktiviteterDtoTjeneste,
                                       AndelerForFaktaOmBeregningTjeneste andelerForFaktaOmBeregningTjeneste) {
        this.dtoTjenester = dtoTjenesteInstance.stream().collect(Collectors.toList());
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.avklarAktiviteterDtoTjeneste = avklarAktiviteterDtoTjeneste;
        this.andelerForFaktaOmBeregningTjeneste = andelerForFaktaOmBeregningTjeneste;
    }

    protected Optional<FaktaOmBeregningDto> lagDto(BeregningsgrunnlagInput input) {
        var ref = input.getBehandlingReferanse();
        Long behandlingId = ref.getBehandlingId();
        Optional<Long> originalBehandlingId = ref.getOriginalBehandlingId();

        FaktaOmBeregningDto faktaOmBeregningDto = new FaktaOmBeregningDto();
        var grunnlagEntitet = input.getBeregningsgrunnlagGrunnlag();
        BeregningAktivitetAggregatEntitet registerAktivitetAggregat = Optional.ofNullable(grunnlagEntitet.getRegisterAktiviteter())
            .orElse(grunnlagEntitet.getGjeldendeAktiviteter());
        Optional<BeregningAktivitetAggregatEntitet> saksbehandletAktivitetAggregat = grunnlagEntitet.getOverstyrteEllerSaksbehandletAktiviteter();
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
            behandlingId, originalBehandlingId, BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        Optional<BeregningAktivitetAggregatEntitet> forrigeRegisterAggregat = forrigeGrunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getRegisterAktiviteter);
        Optional<BeregningAktivitetAggregatEntitet> forrigeSaksbehandletAggregat = forrigeGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getOverstyrteEllerSaksbehandletAktiviteter);

        faktaOmBeregningDto.setAndelerForFaktaOmBeregning(andelerForFaktaOmBeregningTjeneste.lagAndelerForFaktaOmBeregning(input));

        Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon = input.getIayGrunnlag().getArbeidsforholdInformasjon();
        avklarAktiviteterDtoTjeneste.lagAvklarAktiviteterDto(input.getSkjæringstidspunktForBeregning(), registerAktivitetAggregat,
            saksbehandletAktivitetAggregat, forrigeRegisterAggregat, forrigeSaksbehandletAggregat, arbeidsforholdInformasjon, faktaOmBeregningDto);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlagEntitet.getBeregningsgrunnlag().orElseThrow();
        if (skalVurdereFaktaForATFLEllerOverstyre(beregningsgrunnlag, forrigeGrunnlag)) {
            List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
            if (!tilfeller.isEmpty()) {
                faktaOmBeregningDto.setFaktaOmBeregningTilfeller(tilfeller);
                utledDtoerForTilfeller(input, faktaOmBeregningDto);
            }
        }
        return Optional.of(faktaOmBeregningDto);
    }

    private boolean skalVurdereFaktaForATFLEllerOverstyre(BeregningsgrunnlagEntitet beregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        Boolean forrigeBehandlingErOverstyrt = forrigeGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(BeregningsgrunnlagEntitet::isOverstyrt)
            .orElse(false);
        return  (forrigeBehandlingErOverstyrt || beregningsgrunnlag.isOverstyrt()) || !beregningsgrunnlag.getFaktaOmBeregningTilfeller().isEmpty();
    }

    private void utledDtoerForTilfeller(BeregningsgrunnlagInput input,
                                        FaktaOmBeregningDto faktaOmBeregningDto) {
        var ref = input.getBehandlingReferanse();
        Long behandlingId = ref.getBehandlingId();
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt = beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingId, ref.getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.KOFAKBER_UT);
        dtoTjenester.forEach(dtoTjeneste -> dtoTjeneste.lagDto(input, forrigeGrunnlagOpt, faktaOmBeregningDto));
    }
}
