package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class KravDokumentFravær {

    public List<OppgittFraværPeriode> trekkUtFraværMapTilOppgittFraværPeriode(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {
        Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fravær = trekkUtFravær(fraværFraKravdokumenter);
        return mapTilOppgittFraværPeriode(fravær);
    }

    public Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> trekkUtFravær(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {
        Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> resultat = new LinkedHashMap<>();

        fraværFraKravdokumenter.entrySet().stream()
            .sorted(Comparator.comparing(e -> e.getKey().getInnsendingsTidspunkt()))
            .forEachOrdered(dokumentEntry -> {
                KravDokument dok = dokumentEntry.getKey();
                List<VurdertSøktPeriode<OppgittFraværPeriode>> fraværsperioder = dokumentEntry.getValue();

                fraværsperioder.stream().collect(Collectors.groupingBy(AktivitetIdentifikator::lagAktivitetIdentifikator)).forEach(
                    (aktivitetIdent, fraværPerioder) -> {
                        InternArbeidsforholdRef arbeidsforholdRef = aktivitetIdent.getArbeidsgiverArbeidsforhold() != null ? aktivitetIdent.getArbeidsgiverArbeidsforhold().getArbeidsforhold() : InternArbeidsforholdRef.nullRef();
                        var tidslinjeNy = new LocalDateTimeline<>(fraværPerioder.stream()
                            .map(v -> new LocalDateSegment<>(v.getPeriode().toLocalDateInterval(), mapTilFraværHolder(dok.getType(), dok.getInnsendingsTidspunkt(), arbeidsforholdRef, v)))
                            .toList());

                        AktivitetTypeArbeidsgiver aktivitetTypeArbeidsgiver = fjernArbeidsforholdFra(aktivitetIdent);
                        var tidslinjeSammenslått = resultat.getOrDefault(aktivitetTypeArbeidsgiver, (LocalDateTimeline<OppgittFraværHolder>) LocalDateTimeline.EMPTY_TIMELINE);
                        resultat.put(aktivitetTypeArbeidsgiver, tidslinjeSammenslått.combine(tidslinjeNy, this::merge, LocalDateTimeline.JoinStyle.CROSS_JOIN));
                    }
                );
            });

        return komprimerOgFjernTommeTidslinjer(resultat);
    }

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> komprimerOgFjernTommeTidslinjer(Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> mapByAktivitet) {
        return mapByAktivitet.entrySet()
            .stream()
            .filter(e -> !e.getValue().filterValue(fraværHolder -> fraværHolder.søknadGjelder() || fraværHolder.refusjonskravGjelder()).isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().filterValue(fraværHolder -> fraværHolder.søknadGjelder() || fraværHolder.refusjonskravGjelder()).compress()));
    }

    private AktivitetTypeArbeidsgiver fjernArbeidsforholdFra(AktivitetIdentifikator aktivitetIdentifikator) {
        return aktivitetIdentifikator.getArbeidsgiverArbeidsforhold() != null
            ? new AktivitetTypeArbeidsgiver(aktivitetIdentifikator.getAktivitetType(), aktivitetIdentifikator.getArbeidsgiverArbeidsforhold().getArbeidsgiver())
            : new AktivitetTypeArbeidsgiver(aktivitetIdentifikator.getAktivitetType(), null);
    }

    private LocalDateSegment<OppgittFraværHolder> merge(LocalDateInterval di, LocalDateSegment<OppgittFraværHolder> verdi, LocalDateSegment<OppgittFraværHolder> nyVerdi) {
        if (nyVerdi == null) {
            return new LocalDateSegment<>(di, verdi.getValue());
        }
        if (verdi == null) {
            return new LocalDateSegment<>(di, nyVerdi.getValue());
        }
        OppgittFraværHolder eksisterendeVerdier = verdi.getValue();
        OppgittFraværHolder nyeVerdier = nyVerdi.getValue();
        OppgittFraværHolder sammenslått = eksisterendeVerdier.oppdaterMed(nyeVerdier);
        return new LocalDateSegment<>(di, sammenslått);
    }


    private OppgittFraværHolder mapTilFraværHolder(KravDokumentType kravDokumentType, LocalDateTime innsendingstidspunkt, InternArbeidsforholdRef arbeidsforholdRef, VurdertSøktPeriode<OppgittFraværPeriode> vsp) {
        OppgittFraværPeriode fp = vsp.getRaw();
        OppgittFraværVerdi oppgittFraværVerdi = new OppgittFraværVerdi(innsendingstidspunkt, fp.getFraværPerDag(), fp.getFraværÅrsak(), fp.getSøknadÅrsak(), utledUtfall(vsp));
        OppgittFraværHolder oppgittFraværHolder = switch (kravDokumentType) {
            case SØKNAD -> OppgittFraværHolder.fraSøknad(oppgittFraværVerdi);
            case INNTEKTSMELDING -> OppgittFraværHolder.fraRefusjonskrav(arbeidsforholdRef, oppgittFraværVerdi);
            case INNTEKTSMELDING_UTEN_REFUSJONSKRAV -> OppgittFraværHolder.fraImUtenRefusjonskrav(arbeidsforholdRef, oppgittFraværVerdi);
        };
        return oppgittFraværHolder;
    }

    private Utfall utledUtfall(VurdertSøktPeriode<OppgittFraværPeriode> pa) {
        if (Duration.ZERO.equals(pa.getRaw().getFraværPerDag())) {
            return Utfall.OPPFYLT;
        }
        return pa.getUtfall();
    }

    public static List<OppgittFraværPeriode> mapTilOppgittFraværPeriode(Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> oppgittFravær) {
        return oppgittFravær.entrySet().stream()
            .flatMap(e -> mapTilOppgittFraværPeriode(e.getKey(), e.getValue()).stream())
            .toList();
    }

    private static List<OppgittFraværPeriode> mapTilOppgittFraværPeriode(AktivitetTypeArbeidsgiver aktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder> oppgittFraværLocalDateTimeline) {
        return oppgittFraværLocalDateTimeline.stream()
            .flatMap(segment -> mapTilOppgittFraværPeriode(aktivitetTypeArbeidsgiver, segment).stream())
            .toList();
    }

    private static List<OppgittFraværPeriode> mapTilOppgittFraværPeriode(AktivitetTypeArbeidsgiver aktivitetTypeArbeidsgiver, LocalDateSegment<OppgittFraværHolder> oppgittFraværSegment) {
        JournalpostId journalpostId = null;

        List<OppgittFraværPeriode> resultat = new ArrayList<>();
        LocalDate fom = oppgittFraværSegment.getFom();
        LocalDate tom = oppgittFraværSegment.getTom();

        OppgittFraværHolder oppgittFraværHolder = oppgittFraværSegment.getValue();
        if (oppgittFraværHolder.refusjonskravGjelder()) {
            oppgittFraværHolder.getRefusjonskrav().forEach((arbeidsforholdRef, oppgittFraværVerdi) ->
                resultat.add(new OppgittFraværPeriode(journalpostId, fom, tom, aktivitetTypeArbeidsgiver.aktivitetType(), aktivitetTypeArbeidsgiver.arbeidsgiver(), arbeidsforholdRef, oppgittFraværVerdi.fraværPerDag(), oppgittFraværHolder.fraværÅrsak(), oppgittFraværHolder.søknadÅrsak())));
        } else if (oppgittFraværHolder.søknadGjelder()) {
            OppgittFraværVerdi oppgittFraværVerdi = oppgittFraværHolder.getSøknad();
            resultat.add(new OppgittFraværPeriode(journalpostId, fom, tom, aktivitetTypeArbeidsgiver.aktivitetType(), aktivitetTypeArbeidsgiver.arbeidsgiver(), InternArbeidsforholdRef.nullRef(), oppgittFraværVerdi.fraværPerDag(), oppgittFraværHolder.fraværÅrsak(), oppgittFraværHolder.søknadÅrsak()));
        }
        return resultat;
    }

}
