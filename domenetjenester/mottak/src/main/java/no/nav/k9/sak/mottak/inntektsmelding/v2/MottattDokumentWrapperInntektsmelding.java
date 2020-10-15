package no.nav.k9.sak.mottak.inntektsmelding.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.iay.modell.PeriodeAndel;
import no.nav.k9.sak.mottak.inntektsmelding.MapYtelseTypeFraInntektsmelding;
import no.nav.k9.sak.mottak.inntektsmelding.MottattInntektsmeldingWrapper;
import no.nav.k9.sak.typer.JournalpostId;
import no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsforhold;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsgiver;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ArbeidsgiverPrivat;
import no.seres.xsd.nav.inntektsmelding_m._20181211.AvtaltFerieListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.GjenopptakelseNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.GraderingIForeldrepenger;
import no.seres.xsd.nav.inntektsmelding_m._20181211.GraderingIForeldrepengerListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20181211.NaturalytelseDetaljer;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Omsorgspenger;
import no.seres.xsd.nav.inntektsmelding_m._20181211.OpphoerAvNaturalytelseListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Periode;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Refusjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Skjemainnhold;
import no.seres.xsd.nav.inntektsmelding_m._20181211.UtsettelseAvForeldrepenger;
import no.seres.xsd.nav.inntektsmelding_m._20181211.UtsettelseAvForeldrepengerListe;

public class MottattDokumentWrapperInntektsmelding extends MottattInntektsmeldingWrapper<InntektsmeldingM> {

    public MottattDokumentWrapperInntektsmelding(JournalpostId journalpostId, InntektsmeldingM skjema) {
        super(journalpostId, skjema, InntektsmeldingConstants.NAMESPACE);
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

    public List<PeriodeAndel> getOppgittFravær() {
        Optional<Omsorgspenger> omsorgspenger = Optional.ofNullable(getSkjemaInnhold().getOmsorgspenger()).map(JAXBElement::getValue);
        if (omsorgspenger.isEmpty()) {
            return Collections.emptyList();
        }
        var oms = omsorgspenger.get();
        return new MapOmsorgspengerFravær(getJournalpostId(), oms).getAndeler();

    }

    public String getArbeidstaker() {
        return getSkjemaInnhold().getArbeidstakerFnr();
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(getSkjemaInnhold().getArbeidsgiver()).map(JAXBElement::getValue);
    }

    public Optional<ArbeidsgiverPrivat> getArbeidsgiverPrivat() {
        return Optional.ofNullable(getSkjemaInnhold().getArbeidsgiverPrivat()).map(JAXBElement::getValue);
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

    public Optional<String> getVirksomhetsNr() {
        return getArbeidsgiver().map(Arbeidsgiver::getVirksomhetsnummer);
    }

    public boolean getErNærRelasjon() {
        return getSkjemaInnhold().isNaerRelasjon();
    }

    public Optional<LocalDate> getStartDatoPermisjon() {
        FagsakYtelseType ytelseType = getYtelse();
        switch (ytelseType) {
            case PLEIEPENGER_SYKT_BARN:
            case SVANGERSKAPSPENGER:
                return Optional.ofNullable(getSkjemaInnhold().getArbeidsforhold().getValue().getFoersteFravaersdag()).map(JAXBElement::getValue);
            case OMSORGSPENGER:
                return Optional.empty();
            case OPPLÆRINGSPENGER:
                return Optional.empty();
            case PLEIEPENGER_NÆRSTÅENDE:
                return Optional.empty();
            case FORELDREPENGER:
                return Optional.ofNullable(getSkjemaInnhold().getStartdatoForeldrepengeperiode().getValue());
            default:
                return Optional.empty();
        }
    }

    private Skjemainnhold getSkjemaInnhold() {
        return getSkjema().getSkjemainnhold();
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
     * Hvis inntektsmeldingen kommer fra Altinn (innsendingstidspunkt ikke oppgitt), bruker vi
     * tilnæringen "LocalDateTime.now()", selv om riktig innsendingstidspunkt er arkiveringstidspunkt i joark.
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
