package no.nav.k9.sak.mottak.inntektsmelding.v1;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.mottak.inntektsmelding.MapYtelseTypeFraInntektsmelding;
import no.nav.k9.sak.mottak.inntektsmelding.MottattInntektsmeldingWrapper;
import no.nav.k9.sak.typer.PeriodeAndel;
import no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Arbeidsgiver;
import no.seres.xsd.nav.inntektsmelding_m._20180924.AvtaltFerieListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.GjenopptakelseNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.GraderingIForeldrepenger;
import no.seres.xsd.nav.inntektsmelding_m._20180924.GraderingIForeldrepengerListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.InntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20180924.NaturalytelseDetaljer;
import no.seres.xsd.nav.inntektsmelding_m._20180924.OpphoerAvNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Periode;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Refusjon;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Skjemainnhold;
import no.seres.xsd.nav.inntektsmelding_m._20180924.UtsettelseAvForeldrepenger;
import no.seres.xsd.nav.inntektsmelding_m._20180924.UtsettelseAvForeldrepengerListe;
import no.seres.xsd.nav.inntektsmelding_m._20180924.Omsorgspenger;

public class MottattDokumentWrapperInntektsmelding extends MottattInntektsmeldingWrapper<InntektsmeldingM> {

    public MottattDokumentWrapperInntektsmelding(InntektsmeldingM skjema) {
        super(skjema, InntektsmeldingConstants.NAMESPACE);
    }

    public FagsakYtelseType getYtelse() {
        String ytelse = getSkjemaInnhold().getYtelse();
        return MapYtelseTypeFraInntektsmelding.mapYtelseType(ytelse);
    }

    public List<NaturalytelseDetaljer> getGjenopptakelserAvNaturalytelse() {
        return Optional.ofNullable(getSkjemaInnhold().getGjenopptakelseNaturalytelseListe())
            .map(JAXBElement::getValue)
            .map(GjenopptakelseNaturalytelseListe::getNaturalytelseDetaljer)
            .orElse(Collections.emptyList());
    }

    public List<NaturalytelseDetaljer> getOpphørelseAvNaturalytelse() {
        return Optional.ofNullable(getSkjemaInnhold().getOpphoerAvNaturalytelseListe())
            .map(JAXBElement::getValue)
            .map(OpphoerAvNaturalytelseListe::getOpphoerAvNaturalytelse)
            .orElse(Collections.emptyList());
    }

    private Skjemainnhold getSkjemaInnhold() {
        return getSkjema().getSkjemainnhold();
    }
    
    public List<PeriodeAndel> getOppgittFravær() {
        Optional<Omsorgspenger> omsorgspenger = Optional.ofNullable(getSkjemaInnhold().getOmsorgspenger()).map(JAXBElement::getValue);
        if (omsorgspenger.isEmpty()) {
            return Collections.emptyList();
        }
        var oms = omsorgspenger.get();
        Boolean utbetaltPliktige = oms.getHarUtbetaltPliktigeDager().getValue();
        if (utbetaltPliktige != null && !utbetaltPliktige) {
            throw new UnsupportedOperationException("Har ikke støtte for mottak av inntektsmeldinger som ikke har utbetalt pliktige allerede: " + getSkjema());
        }
        return new MapOmsorgspengerFravær(oms).getAndeler();

    }

    public String getArbeidstaker() {
        return getSkjemaInnhold().getArbeidstakerFnr();
    }

    public Arbeidsgiver getArbeidsgiver() {
        return getSkjemaInnhold().getArbeidsgiver();
    }

    public Optional<Arbeidsforhold> getArbeidsforhold() {
        return Optional.ofNullable(getSkjemaInnhold().getArbeidsforhold()).map(JAXBElement::getValue);
    }

    public Optional<String> getArbeidsforholdId() {
        return Optional.ofNullable(getSkjemaInnhold().getArbeidsforhold())
            .map(JAXBElement::getValue)
            .map(Arbeidsforhold::getArbeidsforholdId)
            .map(JAXBElement::getValue);
    }

    public String getVirksomhetsNr() {
        return getArbeidsgiver().getVirksomhetsnummer();
    }

    public boolean getErNærRelasjon() {
        return getSkjemaInnhold().isNaerRelasjon();
    }

    public Optional<LocalDate> getStartDatoPermisjon() {
        FagsakYtelseType ytelseType = getYtelse();
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            return Optional.ofNullable(getSkjemaInnhold().getStartdatoForeldrepengeperiode().getValue());
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            var førsteFraværsdag = getSkjemaInnhold().getArbeidsforhold().getValue().getFoersteFravaersdag();
            return Optional.ofNullable(førsteFraværsdag != null ? førsteFraværsdag.getValue() : null);
        }
        return Optional.empty();
    }

    public Optional<Refusjon> getRefusjon() {
        return Optional.ofNullable(getSkjemaInnhold().getRefusjon()).map(JAXBElement::getValue);
    }

    public List<GraderingIForeldrepenger> getGradering() {
        return getArbeidsforhold().map(Arbeidsforhold::getGraderingIForeldrepengerListe)
            .map(JAXBElement::getValue)
            .map(GraderingIForeldrepengerListe::getGraderingIForeldrepenger)
            .orElse(Collections.emptyList());
    }

    public List<Periode> getAvtaltFerie() {
        return getArbeidsforhold().map(Arbeidsforhold::getAvtaltFerieListe)
            .map(JAXBElement::getValue)
            .map(AvtaltFerieListe::getAvtaltFerie)
            .orElse(Collections.emptyList());
    }

    public List<UtsettelseAvForeldrepenger> getUtsettelser() {
        return getArbeidsforhold().map(Arbeidsforhold::getUtsettelseAvForeldrepengerListe)
            .map(JAXBElement::getValue)
            .map(UtsettelseAvForeldrepengerListe::getUtsettelseAvForeldrepenger)
            .orElse(Collections.emptyList());
    }

    /**
     * Innsendingstidspunkt er ikke oppgitt fra Altinn
     */
    public Optional<LocalDateTime> getInnsendingstidspunkt() {
        return Optional.ofNullable(getSkjemaInnhold().getAvsendersystem().getInnsendingstidspunkt())
            .map(JAXBElement::getValue)
            .map(e -> e);
    }

    public String getAvsendersystem() {
        return getSkjemaInnhold().getAvsendersystem().getSystemnavn();
    }

}
