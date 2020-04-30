package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.opptjening;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening.HåndtereAutomatiskAvslag;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;
import no.nav.k9.sak.typer.Beløp;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerHåndtereAutomatiskAvslag implements HåndtereAutomatiskAvslag {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public OmsorgspengerHåndtereAutomatiskAvslag() {
    }

    @Inject
    public OmsorgspengerHåndtereAutomatiskAvslag(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        if (utbetalingTilBrukerIPerioden(behandling, periode)) {
            regelResultat.getAksjonspunktDefinisjoner().add(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
        }
    }

    private boolean utbetalingTilBrukerIPerioden(Behandling behandling, DatoIntervallEntitet periode) {
        var inntektsmeldingAggregat = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger);
        return inntektsmeldingAggregat.filter(aggregat -> utbetalingTilBrukerIPerioden(aggregat, periode))
            .isPresent();
    }

    private boolean utbetalingTilBrukerIPerioden(InntektsmeldingAggregat inntektsmeldingAggregat,
                                                 DatoIntervallEntitet periode) {
        var inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        var inntektsmeldingSomMatcherUttak = inntektsmeldinger.stream()
            .filter(it -> it.getOppgittFravær().stream()
                .anyMatch(fravære -> periode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fravære.getFom(), fravære.getTom()))))
            .map(Inntektsmelding::getRefusjonBeløpPerMnd)
            .collect(Collectors.toSet());

        if (inntektsmeldingSomMatcherUttak.isEmpty()) {
            return false;
        } else {
            return inntektsmeldingSomMatcherUttak.stream()
                .map(Beløp::getVerdi)
                .anyMatch(BigDecimal.ZERO::equals);
        }
    }
}
