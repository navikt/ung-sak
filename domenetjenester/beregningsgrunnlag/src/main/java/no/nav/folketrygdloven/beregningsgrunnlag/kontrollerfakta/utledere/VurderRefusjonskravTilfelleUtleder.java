package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@ApplicationScoped
public class VurderRefusjonskravTilfelleUtleder implements TilfelleUtleder {

    private InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste;

    public VurderRefusjonskravTilfelleUtleder() {
        // CDI
    }

    @Inject
    public VurderRefusjonskravTilfelleUtleder(InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste) {
        this.inntektsmeldingMedRefusjonTjeneste = inntektsmeldingMedRefusjonTjeneste;
    }

    @Override
    public Optional<FaktaOmBeregningTilfelle> utled(BeregningsgrunnlagInput input, BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag) {
        var ref = input.getBehandlingReferanse();
        if (!inntektsmeldingMedRefusjonTjeneste.finnArbeidsgiverSomHarSøktRefusjonForSent(ref, input.getIayGrunnlag(), beregningsgrunnlagGrunnlag).isEmpty()) {
            return Optional.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT);
        }
        return Optional.empty();
    }
}
