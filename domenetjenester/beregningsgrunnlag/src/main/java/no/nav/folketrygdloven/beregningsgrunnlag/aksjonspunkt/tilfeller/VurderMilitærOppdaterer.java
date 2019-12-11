package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderMilitærDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Hjemmel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_MILITÆR_SIVILTJENESTE")
public class VurderMilitærOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        VurderMilitærDto militærDto = dto.getVurderMilitaer();
        if (militærDto.getHarMilitaer()) {
            leggTilMilitærstatusOgAndelHvisIkkeFinnes(nyttBeregningsgrunnlag);
        } else {
            slettMilitærStatusOgAndelHvisFinnes(nyttBeregningsgrunnlag);
        }

    }

    private void slettMilitærStatusOgAndelHvisFinnes(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        BeregningsgrunnlagEntitet.Builder grunnlagUtenMilitærBuilder = BeregningsgrunnlagEntitet.builder(nyttBeregningsgrunnlag);
        if (harMilitærstatus(nyttBeregningsgrunnlag)) {
            grunnlagUtenMilitærBuilder.fjernAktivitetstatus(AktivitetStatus.MILITÆR_ELLER_SIVIL);
        }
        BeregningsgrunnlagEntitet grunnlagUtenMilitær = grunnlagUtenMilitærBuilder.build();
        grunnlagUtenMilitær.getBeregningsgrunnlagPerioder().forEach(periode -> {
            if (harMilitærandel(periode)) {
                fjernMilitærFraPeriode(grunnlagUtenMilitær, periode);
            }
        });
    }

    private void fjernMilitærFraPeriode(BeregningsgrunnlagEntitet grunnlagUtenMilitær, BeregningsgrunnlagPeriode periode) {
        List<BeregningsgrunnlagPrStatusOgAndel> alleMilitærandeler = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.MILITÆR_ELLER_SIVIL))
            .collect(Collectors.toList());
        BeregningsgrunnlagPeriode.Builder periodeBuilder = BeregningsgrunnlagPeriode.builder(periode);
        alleMilitærandeler.forEach(periodeBuilder::fjernBeregningsgrunnlagPrStatusOgAndel);
        periodeBuilder.build(grunnlagUtenMilitær);
    }

    private void leggTilMilitærstatusOgAndelHvisIkkeFinnes(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(this::leggTilMilitærAndelOmDenIkkeFinnes);
        if (!harMilitærstatus(nyttBeregningsgrunnlag)) {
            BeregningsgrunnlagAktivitetStatus.Builder aktivitetBuilder = BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.MILITÆR_ELLER_SIVIL).medHjemmel(Hjemmel.F_14_7);
            BeregningsgrunnlagEntitet.builder(nyttBeregningsgrunnlag).leggTilAktivitetStatus(aktivitetBuilder);
        }
    }

    private void leggTilMilitærAndelOmDenIkkeFinnes(BeregningsgrunnlagPeriode periode) {
        if (!harMilitærandel(periode)) {
            BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.MILITÆR_ELLER_SIVIL).medInntektskategori(Inntektskategori.ARBEIDSTAKER).build(periode);
        }
    }

    private boolean harMilitærandel(BeregningsgrunnlagPeriode førstePeriode) {
        return førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .anyMatch(andel -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(andel.getAktivitetStatus()));
    }

    private boolean harMilitærstatus(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getAktivitetStatuser().stream()
            .anyMatch(status -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(status.getAktivitetStatus()));
    }

}
