package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;

public class UtledManglendeInntektsmeldingerFraGrunnlagFunction implements BiFunction<BehandlingReferanse, LocalDate, Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>>> {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private FinnEksternReferanse finnEksternReferanse;

    public UtledManglendeInntektsmeldingerFraGrunnlagFunction(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, FinnEksternReferanse finnEksternReferanse) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.finnEksternReferanse = finnEksternReferanse;
    }

    @Override
    public Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> apply(BehandlingReferanse referanse, LocalDate vurderingsdato) {
        final Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(referanse.getBehandlingId());
        return utledPåkrevdeInntektsmeldingerFraGrunnlag(referanse, inntektArbeidYtelseGrunnlag, vurderingsdato);
    }

    private Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> utledPåkrevdeInntektsmeldingerFraGrunnlag(BehandlingReferanse referanse,
                                                                                                      Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, LocalDate vurderingsdato) {
        Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> påkrevdeInntektsmeldinger = new HashMap<>();

        inntektArbeidYtelseGrunnlag.ifPresent(grunnlag -> {

            var filterFør = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(referanse.getAktørId()))
                .før(vurderingsdato.plusDays(1));

            filterFør.getYrkesaktiviteter().stream()
                .filter(ya -> ArbeidType.AA_REGISTER_TYPER.contains(ya.getArbeidType()))
                .filter(ya -> harRelevantAnsettelsesperiodeSomDekkerAngittDato(filterFør, ya, vurderingsdato))
                .forEach(relevantYrkesaktivitet -> {
                    var identifikator = relevantYrkesaktivitet.getArbeidsgiver();
                    var arbeidsforholdRef = getRef(relevantYrkesaktivitet);

                    if (påkrevdeInntektsmeldinger.containsKey(identifikator)) {
                        påkrevdeInntektsmeldinger.get(identifikator).add(arbeidsforholdRef);
                    } else {
                        final Set<EksternArbeidsforholdRef> arbeidsforholdSet = new LinkedHashSet<>();
                        arbeidsforholdSet.add(arbeidsforholdRef);
                        påkrevdeInntektsmeldinger.put(identifikator, arbeidsforholdSet);
                    }
                });
        });
        return påkrevdeInntektsmeldinger;
    }

    private EksternArbeidsforholdRef getRef(Yrkesaktivitet relevantYrkesaktivitet) {
        return finnEksternReferanse.apply(relevantYrkesaktivitet.getArbeidsgiver(), relevantYrkesaktivitet.getArbeidsforholdRef());
    }

    private boolean harRelevantAnsettelsesperiodeSomDekkerAngittDato(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktivitet, LocalDate dato) {
        if (yrkesaktivitet.erArbeidsforhold()) {
            List<AktivitetsAvtale> ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
            return ansettelsesPerioder.stream().anyMatch(avtale -> avtale.getPeriode().inkluderer(dato));
        }
        return false;
    }
}
