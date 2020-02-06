package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT")
public class VurderRefusjonTilfelleOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste;

    VurderRefusjonTilfelleOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonTilfelleOppdaterer(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                            InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste) {

        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.inntektsmeldingMedRefusjonTjeneste = inntektsmeldingMedRefusjonTjeneste;
    }


    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        List<RefusjonskravPrArbeidsgiverVurderingDto> gyldighetPrArbeidsgiver = dto.getRefusjonskravGyldighet();
        LocalDate frist = nyttBeregningsgrunnlag.getSkjæringstidspunkt();
        BeregningRefusjonOverstyringerEntitet beregningRefusjonOverstyringer = map(gyldighetPrArbeidsgiver, frist, behandlingReferanse);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), beregningRefusjonOverstyringer);
    }

    private BeregningRefusjonOverstyringerEntitet map(List<RefusjonskravPrArbeidsgiverVurderingDto> dto, LocalDate frist, BehandlingReferanse behandlingReferanse) {
        BeregningRefusjonOverstyringerEntitet.Builder builder = BeregningRefusjonOverstyringerEntitet.builder();
        for (RefusjonskravPrArbeidsgiverVurderingDto vurderingDto : dto) {
            Arbeidsgiver arbeidsgiver = finnArbeidsgiver(vurderingDto.getArbeidsgiverId());
            if (vurderingDto.isSkalUtvideGyldighet()) {
                builder.leggTilOverstyring(new BeregningRefusjonOverstyringEntitet(arbeidsgiver, frist));
            } else {
                Optional<LocalDate> førsteLovligeDato = inntektsmeldingMedRefusjonTjeneste.finnFørsteLovligeDatoForRefusjonFørOverstyring(behandlingReferanse, arbeidsgiver);
                førsteLovligeDato.ifPresent(dato -> builder.leggTilOverstyring(new BeregningRefusjonOverstyringEntitet(arbeidsgiver, dato)));
            }
        }
        return builder.build();
    }

    private Arbeidsgiver finnArbeidsgiver(String identifikator) {
        if (OrgNummer.erGyldigOrgnr(identifikator)) {
            return Arbeidsgiver.virksomhet(identifikator);
        }
        return Arbeidsgiver.fra(new AktørId(identifikator));
    }

}
