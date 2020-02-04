package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.KontrollerFaktaBeregningFrilanserTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@ApplicationScoped
public class ArbeidstakerOgFrilanserISammeOrganisasjonTilfelleUtleder implements TilfelleUtleder {

    @Override
    public Optional<FaktaOmBeregningTilfelle> utled(BeregningsgrunnlagInput input,
                                                    BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag) {
        var ref = input.getBehandlingReferanse();
        boolean erATFLISammeOrg = KontrollerFaktaBeregningFrilanserTjeneste.erBrukerArbeidstakerOgFrilanserISammeOrganisasjon(
            ref.getAktørId(), beregningsgrunnlagGrunnlag.getBeregningsgrunnlag().orElse(null), input.getIayGrunnlag());
        return erATFLISammeOrg ? Optional.of(FaktaOmBeregningTilfelle.VURDER_AT_OG_FL_I_SAMME_ORGANISASJON) : Optional.empty();
    }

}
