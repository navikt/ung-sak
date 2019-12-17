package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.AvklarteAktiviteterDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.BeregningsaktivitetLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetHandlingType;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetOverstyringEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;

@ApplicationScoped
public class AvklarAktiviteterHåndterer {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    public AvklarAktiviteterHåndterer() {
        // For CDI
    }

    @Inject
    AvklarAktiviteterHåndterer(BeregningsgrunnlagRepository beregningsgrunnlagRepository, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public void håndter(AvklarteAktiviteterDto dto, BehandlingReferanse referanse) {
        Long behandlingId = referanse.getBehandlingId();
        BeregningsgrunnlagGrunnlagEntitet grunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId)
        .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler BeregningsgrunnlagGrunnlagEntitet"));
        List<BeregningsaktivitetLagreDto> handlingListe = dto.getBeregningsaktivitetLagreDtoList();
        BeregningAktivitetAggregatEntitet registerAktiviteter = grunnlag.getRegisterAktiviteter();
        BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter = SaksbehandletBeregningsaktivitetTjeneste.lagSaksbehandletVersjon(registerAktiviteter, handlingListe);
        beregningsgrunnlagRepository.lagreSaksbehandledeAktiviteter(behandlingId, saksbehandledeAktiviteter, BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
    }

    public void håndterOverstyring(OverstyrBeregningsaktiviteterDto dto, Long behandlingId) {
        BeregningAktivitetOverstyringerEntitet.Builder overstyringAggregatBuilder = BeregningAktivitetOverstyringerEntitet.builder();
        dto.getBeregningsaktivitetLagreDtoList()
            .forEach(overstyrtDto -> {
                BeregningAktivitetOverstyringEntitet overstyring = lagOverstyring(overstyrtDto);
                overstyringAggregatBuilder.leggTilOverstyring(overstyring);
            });
        beregningsgrunnlagRepository.lagre(behandlingId, overstyringAggregatBuilder.build());
    }

    private BeregningAktivitetOverstyringEntitet lagOverstyring(BeregningsaktivitetLagreDto overstyrtDto) {
        return BeregningAktivitetOverstyringEntitet.builder()
            .medHandling(mapTilHandlingType(overstyrtDto))
            .medArbeidsforholdRef(InternArbeidsforholdRef.ref(overstyrtDto.getArbeidsforholdRef()))
            .medArbeidsgiver(arbeidsgiverTjeneste.hentArbeidsgiver(overstyrtDto.getOppdragsgiverOrg(),
                overstyrtDto.getArbeidsgiverIdentifikator()))
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(overstyrtDto.getFom(), overstyrtDto.getTom()))
            .medOpptjeningAktivitetType(overstyrtDto.getOpptjeningAktivitetType())
            .build();
    }

    private BeregningAktivitetHandlingType mapTilHandlingType(BeregningsaktivitetLagreDto overstyrtDto) {
        if (overstyrtDto.getSkalBrukes()) {
            return BeregningAktivitetHandlingType.BENYTT;
        } else {
            return BeregningAktivitetHandlingType.IKKE_BENYTT;
        }
    }

}
