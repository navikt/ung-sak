package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.tilfeller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT")
public class VurderRefusjonHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    VurderRefusjonHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
    }

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        for (RefusjonskravPrArbeidsgiverVurderingDto vurderingDto : dto.getRefusjonskravGyldighet()) {
            Arbeidsgiver arbeidsgiver = finnArbeidsgiver(vurderingDto.getArbeidsgiverId());
            LocalDate frist = nyttBeregningsgrunnlag.getSkjæringstidspunkt();
            Boolean forrige = finnForrigeVerdi(forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getRefusjonOverstyringer), arbeidsgiver, frist);
            lagHistorikkInnslag(
                Boolean.TRUE.equals(vurderingDto.isSkalUtvideGyldighet()),
                forrige,
                arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(arbeidsgiver, iayGrunnlag.getArbeidsforholdOverstyringer()),
                tekstBuilder);
        }
    }

    private Boolean finnForrigeVerdi(Optional<BeregningRefusjonOverstyringerEntitet> forrigeBeregningRefusjonOverstyringer, Arbeidsgiver arbeidsgiver, LocalDate frist) {
        return forrigeBeregningRefusjonOverstyringer.map(BeregningRefusjonOverstyringerEntitet::getRefusjonOverstyringer)
            .orElse(Collections.emptyList())
            .stream()
            .filter(beregningRefusjonOverstyring -> beregningRefusjonOverstyring.getArbeidsgiver().equals(arbeidsgiver))
            .findFirst()
            .map(beregningRefusjonOverstyring -> beregningRefusjonOverstyring.getFørsteMuligeRefusjonFom().equals(frist))
            .orElse(null);
    }

    private Arbeidsgiver finnArbeidsgiver(String identifikator) {
        if (OrgNummer.erGyldigOrgnr(identifikator)) {
            return Arbeidsgiver.virksomhet(identifikator);
        }
        return Arbeidsgiver.fra(new AktørId(identifikator));
    }

    private void lagHistorikkInnslag(Boolean nyVerdi, Boolean forrige, String arbeidsgivernavn, HistorikkInnslagTekstBuilder tekstBuilder) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.NY_REFUSJONSFRIST, arbeidsgivernavn, forrige, nyVerdi);
    }

}
