package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.VurderMottarYtelseTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.ArbeidstakerandelUtenIMMottarYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.MottarYtelseDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_MOTTAR_YTELSE")
public class MottarYtelseOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    MottarYtelseOppdaterer() {
        // For CDI
    }

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                         Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        MottarYtelseDto mottarYtelseDto = dto.getMottarYtelse();
        if (VurderMottarYtelseTjeneste.erFrilanser(nyttBeregningsgrunnlag) && mottarYtelseDto.getFrilansMottarYtelse() != null) {
            settMottarYtelseForFrilans(nyttBeregningsgrunnlag, mottarYtelseDto);
        }
        nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList()
                .forEach(andel -> settMottarYtelseForAndel(mottarYtelseDto, nyttBeregningsgrunnlag, andel));
    }

    private void settMottarYtelseForAndel(MottarYtelseDto mottarYtelseDto, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, BeregningsgrunnlagPrStatusOgAndel andel) {
        Optional<Boolean> mottarYtelseVerdiForAndel = mottarYtelseDto.getArbeidstakerUtenIMMottarYtelse().stream()
            .filter(mottarYtelseAndel -> mottarYtelseAndel.getAndelsnr() == andel.getAndelsnr())
            .findFirst().map(ArbeidstakerandelUtenIMMottarYtelseDto::getMottarYtelse);
        mottarYtelseVerdiForAndel
            .ifPresent(mottarYtelse -> settMottarYtelseVerdiForAndelerMedArbeidsforholdUtenIM(nyttBeregningsgrunnlag, andel, mottarYtelse));
    }

    private void settMottarYtelseVerdiForAndelerMedArbeidsforholdUtenIM(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, BeregningsgrunnlagPrStatusOgAndel andel, boolean mottarYtelse) {
        nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(nyAndel -> nyAndel.gjelderSammeArbeidsforhold(andel))
            .forEach(nyAndel -> BeregningsgrunnlagPrStatusOgAndel.builder(nyAndel)
                .medMottarYtelse(mottarYtelse, nyAndel.getAktivitetStatus()));
    }

    private void settMottarYtelseForFrilans(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, MottarYtelseDto mottarYtelseDto) {
        nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream().flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().erFrilanser())
            .forEach(andel -> BeregningsgrunnlagPrStatusOgAndel.builder(andel)
                .medMottarYtelse(mottarYtelseDto.getFrilansMottarYtelse(), andel.getAktivitetStatus()));
    }

}
