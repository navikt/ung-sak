package no.nav.folketrygdloven.beregningsgrunnlag.refusjon;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.FinnYrkesaktiviteterForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.FinnAnsettelsesPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.FinnFørsteDagEtterBekreftetPermisjon;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.RefusjonskravDato;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;

@ApplicationScoped
public class InntektsmeldingMedRefusjonTjeneste {

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    InntektsmeldingMedRefusjonTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InntektsmeldingMedRefusjonTjeneste(InntektsmeldingTjeneste inntektsmeldingTjeneste) {

        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    public Map<Arbeidsgiver, LocalDate> finnFørsteInntektsmeldingMedRefusjon(BehandlingReferanse behandlingReferanse) {
        var refusjonskravDatoer = inntektsmeldingTjeneste.hentAlleRefusjonskravDatoerForFagsak(behandlingReferanse.getSaksnummer());
        return refusjonskravDatoer.stream().collect(Collectors.toMap(RefusjonskravDato::getArbeidsgiver, RefusjonskravDato::getFørsteInnsendingAvRefusjonskrav));
    }

    public Set<Arbeidsgiver> finnArbeidsgiverSomHarSøktRefusjonForSent(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        YrkesaktivitetFilter filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()));

        Collection<Yrkesaktivitet> yrkesaktiviteterForBeregning = FinnYrkesaktiviteterForBeregningTjeneste.finnYrkesaktiviteter(behandlingReferanse, iayGrunnlag, grunnlag);
        var refusjonskravDatoer = inntektsmeldingTjeneste.hentAlleRefusjonskravDatoerForFagsak(behandlingReferanse.getSaksnummer());
        Map<Yrkesaktivitet, Optional<RefusjonskravDato>> yrkesaktivitetDatoMap = map(yrkesaktiviteterForBeregning, refusjonskravDatoer);
        LocalDate skjæringstidspunktBeregning = grunnlag.getBeregningsgrunnlag().map(BeregningsgrunnlagEntitet::getSkjæringstidspunkt)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Skal ha beregningsgrunnlag"));

        Set<Arbeidsgiver> resultat = new HashSet<>();

        for (Map.Entry<Yrkesaktivitet, Optional<RefusjonskravDato>> entry : yrkesaktivitetDatoMap.entrySet()) {
            if (entry.getValue().isPresent()) {
                Yrkesaktivitet yrkesaktivitet = entry.getKey();
                Periode ansettelsesPeriode = FinnAnsettelsesPeriode.finnMinMaksPeriode(filter.getAnsettelsesPerioder(yrkesaktivitet), skjæringstidspunktBeregning)
                    .orElseThrow(() -> new IllegalStateException("Utviklerfeil!"));

                Optional<LocalDate> dagEtterBekreftetPermisjon = FinnFørsteDagEtterBekreftetPermisjon.finn(iayGrunnlag, yrkesaktivitet, ansettelsesPeriode, skjæringstidspunktBeregning);
                if (dagEtterBekreftetPermisjon.isEmpty()) {
                    continue;
                }
                RefusjonskravDato refusjonsdato = entry.getValue().get();
                if (!harArbeidsgiverSendtInnInntektsmeldingMedGyldigRefusjonskrav(refusjonsdato.getFørsteInnsendingAvRefusjonskrav(),
                    refusjonsdato.getFørsteDagMedRefusjonskrav().withDayOfMonth(refusjonsdato.getFørsteDagMedRefusjonskrav().getDayOfMonth()).plusMonths(3))) {
                    resultat.add(yrkesaktivitet.getArbeidsgiver());
                }
            }
        }
        return resultat;
    }

    public Optional<LocalDate> finnFørsteLovligeDatoForRefusjonFørOverstyring(BehandlingReferanse behandlingReferanse, Arbeidsgiver arbeidsgiver) {
        Map<Arbeidsgiver, LocalDate> førsteInntektsmeldingMap = finnFørsteInntektsmeldingMedRefusjon(behandlingReferanse);
        LocalDate innsendingstidspunkt = førsteInntektsmeldingMap.get(arbeidsgiver);
        if (innsendingstidspunkt != null) {
            return Optional.of(innsendingstidspunkt.withDayOfMonth(1).minusMonths(3));
        }
        return Optional.empty();
    }

    private Map<Yrkesaktivitet, Optional<RefusjonskravDato>> map(Collection<Yrkesaktivitet> yrkesaktiviteterForBeregning, List<RefusjonskravDato> refusjonskravDatoList) {
        return yrkesaktiviteterForBeregning.stream()
            .collect(Collectors.toMap(y -> y, finnRefusjonskravDato(refusjonskravDatoList)));
    }

    private Function<Yrkesaktivitet, Optional<RefusjonskravDato>> finnRefusjonskravDato(List<RefusjonskravDato> refusjonskravDatoList) {
        return y -> refusjonskravDatoList.stream().filter(rd -> y.getArbeidsgiver().equals(rd.getArbeidsgiver())).findFirst();
    }

    private boolean harArbeidsgiverSendtInnInntektsmeldingMedGyldigRefusjonskrav(LocalDate førsteInnsendingAvRefusjonskrav, LocalDate frist) {
        return førsteInnsendingAvRefusjonskrav.isBefore(frist);
    }

}
