package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.opptjening;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening.HåndtereAutomatiskAvslag;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;

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
    public void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode, List<OpptjeningAktivitet> opptjeningAktivteter) {
        if (utbetalingTilBrukerIPerioden(behandling, periode)) {
            regelResultat.getAksjonspunktDefinisjoner().add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET));
        }
    }

    protected boolean erMidlertidigInaktiv(DatoIntervallEntitet periode, List<OpptjeningAktivitet> opptjeningAktiviteter) {
        DatoIntervallEntitet midlertidigInaktivPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusDays(28), periode.getFomDato());

        if (opptjeningAktiviteter == null) {
            throw new IllegalStateException("Fant ingen opptjeningsaktiviteter");
        }

        Optional<OpptjeningAktivitet> overlapp = opptjeningAktiviteter.stream()
            .filter(opptjeningAktivitet -> opptjeningAktivitet.getDatoIntervallEntitet().overlapper(midlertidigInaktivPeriode))
            .filter(opptjeningAktivitet -> opptjeningAktivitet.getKlassifisering() == OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT)
            .findFirst();
        return overlapp.isPresent();
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
            .filter(it -> it.getOppgittFravær() != null &&
                it.getOppgittFravær().stream()
                    .anyMatch(fravære -> periode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fravære.getFom(), fravære.getTom()))))
            .collect(Collectors.toSet());

        if (inntektsmeldingSomMatcherUttak.isEmpty()) {
            return false;
        } else {
            return inntektsmeldingSomMatcherUttak.stream()
                .anyMatch(v -> {
                    var refusjon = v.getRefusjonBeløpPerMnd();
                    return refusjon == null || refusjon.getVerdi() == null || BigDecimal.ZERO.compareTo(refusjon.getVerdi()) == 0;
                });
        }
    }
}
