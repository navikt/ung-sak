package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.RefusjonskravSomKommerForSentDto;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;

@ApplicationScoped
class VurderRefusjonTilfelleDtoTjeneste implements FaktaOmBeregningTilfelleDtoTjeneste {

    private InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;


    public VurderRefusjonTilfelleDtoTjeneste() {
        // CDI
    }

    @Inject
    public VurderRefusjonTilfelleDtoTjeneste(InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste,
                                             ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.inntektsmeldingMedRefusjonTjeneste = inntektsmeldingMedRefusjonTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    @Override
    public void lagDto(BeregningsgrunnlagInput input, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt, FaktaOmBeregningDto faktaOmBeregningDto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = input.getBeregningsgrunnlag();
        List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        if (!tilfeller.contains(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT)) {
            return;
        }
        List<RefusjonskravSomKommerForSentDto> refusjonskravSomKommerForSentList = lagListeMedKravSomKommerForSent(input, forrigeGrunnlagOpt);
        faktaOmBeregningDto.setRefusjonskravSomKommerForSentListe(refusjonskravSomKommerForSentList);
    }



    private List<RefusjonskravSomKommerForSentDto> lagListeMedKravSomKommerForSent(BeregningsgrunnlagInput input,
                                                                                   Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt) {
        List<BeregningRefusjonOverstyringEntitet> refusjonOverstyringer = forrigeGrunnlagOpt
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getRefusjonOverstyringer)
            .map(BeregningRefusjonOverstyringerEntitet::getRefusjonOverstyringer)
            .orElse(Collections.emptyList());


        var ref = input.getBehandlingReferanse();
        var beregnGrunnlag = input.getBeregningsgrunnlagGrunnlag();
        var iayGrunnlag = input.getIayGrunnlag();
        return inntektsmeldingMedRefusjonTjeneste.finnArbeidsgiverSomHarSøktRefusjonForSent(ref, iayGrunnlag, beregnGrunnlag)
            .stream()
            .map(arbeidsgiver -> {
                RefusjonskravSomKommerForSentDto dto = new RefusjonskravSomKommerForSentDto();
                dto.setArbeidsgiverId(arbeidsgiver.getIdentifikator());
                dto.setArbeidsgiverVisningsnavn(arbeidsgiverTjeneste.hent(arbeidsgiver).getNavn());
                LocalDate skjæringstidspunkt = beregnGrunnlag.getBeregningsgrunnlag().map(BeregningsgrunnlagEntitet::getSkjæringstidspunkt).orElseThrow(() -> new IllegalStateException("Skal ha beregningsgrunnlag med skjæringstidspunkt"));
                dto.setErRefusjonskravGyldig(sjekkStatusPåRefusjon(arbeidsgiver.getIdentifikator(), refusjonOverstyringer, skjæringstidspunkt));

                return dto;
            }).collect(Collectors.toList());
    }

    private Boolean sjekkStatusPåRefusjon(String identifikator,
                                          List<BeregningRefusjonOverstyringEntitet> refusjonOverstyringer,
                                          LocalDate skjæringstidspunkt) {
        Optional<BeregningRefusjonOverstyringEntitet> statusOpt = refusjonOverstyringer
            .stream()
            .filter(refusjonOverstyring -> refusjonOverstyring.getArbeidsgiver().getIdentifikator().equals(identifikator))
            .findFirst();
        if (statusOpt.isEmpty() && refusjonOverstyringer.isEmpty()) {
            return null;
        }
        return statusOpt.isPresent() && statusOpt.get().getFørsteMuligeRefusjonFom().isEqual(skjæringstidspunkt);
    }
}
