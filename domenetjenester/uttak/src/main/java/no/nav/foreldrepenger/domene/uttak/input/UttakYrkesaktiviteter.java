package no.nav.foreldrepenger.domene.uttak.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class UttakYrkesaktiviteter {
    private UttakInput input;

    public UttakYrkesaktiviteter(UttakInput input) {
        this.input = input;
    }

    private List<Yrkesaktivitet> hentYrkesAktiviteterOrdinærtArbeidsforhold(UttakInput input) {
        InntektArbeidYtelseGrunnlag grunnlag = input.getIayGrunnlag();
        if (grunnlag == null) {
            return Collections.emptyList();
        }
        Collection<BeregningsgrunnlagStatusPeriode> andeler = input.getBeregningsgrunnlagStatusPerioder();
        var ref = input.getBehandlingReferanse();
        LocalDate skjæringstidspunkt = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();

        AktørId aktørId = ref.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).etter(skjæringstidspunkt);

        return filter.getYrkesaktiviteter()
            .stream()
            .filter(yrkesaktivitet -> skalYrkesaktivitetTasMed(yrkesaktivitet, andeler))
            .collect(Collectors.toList());
    }

    private List<Yrkesaktivitet> hentYrkesAktiviteterFrilans(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var grunnlag = input.getIayGrunnlag();
        var andeler = input.getBeregningsgrunnlagStatusPerioder();

        if (grunnlag == null || (andeler == null || andeler.isEmpty())) {
            return Collections.emptyList();
        }
        var skjæringstidspunkt = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();

        var aktørId = ref.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).etter(skjæringstidspunkt);

        return filter.getFrilansOppdrag().stream()
            .filter(yrkesaktivitet -> skalYrkesaktivitetTasMed(yrkesaktivitet, andeler))
            .collect(Collectors.toList());
    }

    public List<Yrkesaktivitet> hentAlleYrkesaktiviteter() {
        List<Yrkesaktivitet> aktiviteter = hentYrkesAktiviteterOrdinærtArbeidsforhold(input);
        aktiviteter.addAll(hentYrkesAktiviteterFrilans(input));
        return aktiviteter;
    }

    private boolean skalYrkesaktivitetTasMed(Yrkesaktivitet yrkesaktivitet, Collection<BeregningsgrunnlagStatusPeriode> statusPerioder) {
        return statusPerioder.stream().anyMatch(bsp -> skalYrkesaktivitetTasMed(yrkesaktivitet, bsp));
    }

    private boolean skalYrkesaktivitetTasMed(Yrkesaktivitet yrkesaktivitet, BeregningsgrunnlagStatusPeriode bsp) {
        var arbeidsgiver = bsp.getArbeidsgiver().orElse(null);
        var arbeidsforhold = bsp.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef());
        var arbeidsgiver2 = yrkesaktivitet.getArbeidsgiver();
        var arbeidsforhold2 = Optional.ofNullable(yrkesaktivitet.getArbeidsforholdRef()).orElse(InternArbeidsforholdRef.nullRef());
        if (arbeidsgiver == null || arbeidsgiver2 == null) {
            return false;
        }

        boolean taMed = Objects.equals(arbeidsgiver, arbeidsgiver2)
            && arbeidsforhold.gjelderFor(arbeidsforhold2);
        return taMed;
    }

    public BigDecimal finnStillingsprosentOrdinærtArbeid(String arbeidsgiverIdentifikator,
                                                         InternArbeidsforholdRef arbeidsforholdId,
                                                         LocalDate dato) {
        List<Yrkesaktivitet> yrkesAktiviteter = hentYrkesAktiviteterOrdinærtArbeidsforhold(input);
        var ref = input.getBehandlingReferanse();
        return finnStillingsprosentOrdinærtArbeid(arbeidsgiverIdentifikator, arbeidsforholdId, yrkesAktiviteter, dato, ref.getSkjæringstidspunkt());
    }

    private BigDecimal finnStillingsprosentOrdinærtArbeid(String arbeidsgiverIdentifikator,
                                                          InternArbeidsforholdRef arbeidsforholdId,
                                                          List<Yrkesaktivitet> yrkesaktivitetList,
                                                          LocalDate dato,
                                                          Skjæringstidspunkt skjæringstidspunkt) {

        var filter0 = new YrkesaktivitetFilter(null, yrkesaktivitetList);
        List<Yrkesaktivitet> yaMedAnsettelsesperiodePåDato = yaMedAnsettelsesperiodePåDato(filter0, arbeidsgiverIdentifikator, arbeidsforholdId,
            yrkesaktivitetList, dato);

        var filter = new YrkesaktivitetFilter(null, yaMedAnsettelsesperiodePåDato).etter(skjæringstidspunkt.getUtledetSkjæringstidspunkt());

        BigDecimal sum = BigDecimal.ZERO;
        for (Yrkesaktivitet ya : filter.getAlleYrkesaktiviteter()) {
            var aktivitetsAvtaler = filter.getAktivitetsAvtalerForArbeid(ya);
            if (aktivitetsAvtaler.isEmpty()) {
                String melding = "Forventer minst en aktivitetsavtale ved dato " + dato.toString() +
                    " i yrkesaktivitet" + ya.toString() +
                    " med ansettelsesperioder " + filter.getAnsettelsesPerioder(ya).toString() +
                    " og alle aktivitetsavtaler " + aktivitetsAvtaler.toString();
                throw new IllegalStateException(melding);
            }
            sum = sum.add(finnStillingsprosent(aktivitetsAvtaler, dato));
        }

        return sum;
    }

    private List<Yrkesaktivitet> yaMedAnsettelsesperiodePåDato(YrkesaktivitetFilter filter,
                                                               String arbeidsgiverIdentifikator,
                                                               InternArbeidsforholdRef arbeidsforholdId,
                                                               List<Yrkesaktivitet> yrkesaktivitetList,
                                                               LocalDate dato) {
        return yrkesaktivitetList.stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .filter(ya -> riktigDato(dato, ansettelsePeriodeForYrkesaktivitet(filter, ya)))
            .filter(ya -> Objects.equals(ya.getArbeidsgiver().getIdentifikator(), arbeidsgiverIdentifikator))
            .filter(ya -> ya.getArbeidsforholdRef().gjelderFor(arbeidsforholdId == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdId))
            .collect(Collectors.toList());
    }

    private BigDecimal finnStillingsprosent(Collection<AktivitetsAvtale> aktivitetsAvtaler, LocalDate dato) {
        AktivitetsAvtale aktivitetPåDato = finnAktivitetPåDato(aktivitetsAvtaler, dato);
        if (aktivitetPåDato.getProsentsats() == null) {
            return BigDecimal.ZERO;
        }
        return aktivitetPåDato.getProsentsats().getVerdi();
    }

    private AktivitetsAvtale finnAktivitetPåDato(Collection<AktivitetsAvtale> aktivitetsAvtaler, LocalDate dato) {
        Optional<AktivitetsAvtale> overlapper = aktivitetsAvtaler.stream().filter(aa -> riktigDato(dato, aa)).findFirst();
        if (overlapper.isPresent()) {
            return overlapper.get();
        }

        // Ingen avtaler finnes på dato. Bruker nærmeste avtale. Kommer av dårlig datakvalitet i registerne
        List<AktivitetsAvtale> sortert = sortertPåDato(aktivitetsAvtaler);
        AktivitetsAvtale førsteAktivitetsavtale = sortert.get(0);
        // Hull i starten av ansettelsesperioden
        if (dato.isBefore(førsteAktivitetsavtale.getPeriode().getFomDato())) {
            return førsteAktivitetsavtale;
        }
        // Hull på slutten av ansettelsesperioden
        return sortert.get(aktivitetsAvtaler.size() - 1);

    }

    private List<AktivitetsAvtale> sortertPåDato(Collection<AktivitetsAvtale> aktivitetsAvtaler) {
        return aktivitetsAvtaler.stream()
            .sorted(Comparator.comparing(aktivitetsAvtale -> aktivitetsAvtale.getPeriode().getFomDato()))
            .collect(Collectors.toList());
    }

    private List<AktivitetsAvtale> ansettelsePeriodeForYrkesaktivitet(YrkesaktivitetFilter filter, Yrkesaktivitet ya) {
        List<AktivitetsAvtale> ansettelsesPerioder = filter.getAnsettelsesPerioder(ya);
        if (ansettelsesPerioder.isEmpty()) {
            throw new IllegalStateException("Forventet at alle yrkesaktiviteter har en ansettelsesperiode");
        }
        return ansettelsesPerioder;
    }

    private boolean riktigDato(LocalDate dato, List<AktivitetsAvtale> avtaler) {
        return avtaler.stream().anyMatch(avtale -> riktigDato(dato, avtale));
    }

    private boolean riktigDato(LocalDate dato, AktivitetsAvtale avtale) {
        return (avtale.getPeriode().getFomDato().isEqual(dato) || avtale.getPeriode().getFomDato().isBefore(dato)) &&
            (avtale.getPeriode().getTomDato().isEqual(dato) || avtale.getPeriode().getTomDato().isAfter(dato));
    }
}
