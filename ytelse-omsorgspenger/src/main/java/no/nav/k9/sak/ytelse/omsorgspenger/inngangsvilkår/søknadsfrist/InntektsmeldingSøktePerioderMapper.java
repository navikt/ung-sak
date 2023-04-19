package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Periode;
import no.nav.k9.sak.domene.iay.modell.PeriodeAndel;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class InntektsmeldingSøktePerioderMapper {

    private boolean zeroRefErKrav;
    private boolean nullHelgILangePerioder;

    @Inject
    public InntektsmeldingSøktePerioderMapper(@KonfigVerdi(value = "OMS_ZERO_REFUSJON_ER_KRAV", defaultVerdi = "false") boolean zeroRefErKrav,
                                              @KonfigVerdi(value = "OMP_NULL_HELG_I_KRAVPERIODER", defaultVerdi = "false") boolean nullHelgILangePerioder) {
        this.zeroRefErKrav = zeroRefErKrav;
        this.nullHelgILangePerioder = nullHelgILangePerioder;
    }

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> mapTilSøktePerioder(Set<Inntektsmelding> inntektsmeldinger) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> result = new HashMap<>();
        inntektsmeldinger.forEach(it -> mapTilSøktePerioder(result, it));
        return result;
    }

    private void mapTilSøktePerioder(Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> result, no.nav.k9.sak.domene.iay.modell.Inntektsmelding it) {
        if (nullHelgILangePerioder) {
            KravDokumentType kravDokumentType = utledKravDokumentType(it);
            result.put(new KravDokument(it.getJournalpostId(), it.getInnsendingstidspunkt(), kravDokumentType),
                it.getOppgittFravær()
                    .stream()
                    .flatMap(pa -> fjernHelgGittRegel(kravDokumentType, pa).stream())
                    .map(pa -> new OppgittFraværPeriode(it.getJournalpostId(), pa.getFom(), pa.getTom(), UttakArbeidType.ARBEIDSTAKER, it.getArbeidsgiver(), it.getArbeidsforholdRef(), pa.getVarighetPerDag(), FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT))
                    .map(op -> new SøktPeriode<>(op.getPeriode(), op.getAktivitetType(), op.getArbeidsgiver(), op.getArbeidsforholdRef(), op))
                    .toList());
        } else {
            result.put(new KravDokument(it.getJournalpostId(), it.getInnsendingstidspunkt(), utledKravDokumentType(it)),
                it.getOppgittFravær()
                    .stream()
                    .map(pa -> new OppgittFraværPeriode(it.getJournalpostId(), pa.getFom(), pa.getTom(), UttakArbeidType.ARBEIDSTAKER, it.getArbeidsgiver(), it.getArbeidsforholdRef(), pa.getVarighetPerDag(), FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT))
                    .map(op -> new SøktPeriode<>(op.getPeriode(), op.getAktivitetType(), op.getArbeidsgiver(), op.getArbeidsforholdRef(), op))
                    .collect(Collectors.toList()));
        }
    }

    private List<PeriodeAndel> fjernHelgGittRegel(KravDokumentType kravDokumentType, PeriodeAndel periodeAndel) {
        if (kravDokumentType != KravDokumentType.INNTEKTSMELDING) {
            return List.of(periodeAndel);
        }
        long dagerIPerioden = ChronoUnit.DAYS.between(periodeAndel.getPeriode().getFom(), periodeAndel.getPeriode().getTom()) + 1; //+1 siden between-metoden er eksklusive slutt, men vi skal telle med siste dag
        if (dagerIPerioden < 7) {
            return List.of(periodeAndel);
        }
        boolean delvisFravær = periodeAndel.getVarighetPerDag() != null && !Duration.ofHours(7).plusMinutes(30).equals(periodeAndel.getVarighetPerDag());
        if (delvisFravær) {
            return List.of(periodeAndel);
        }
        LocalDateTimeline<Boolean> tidslinje = new LocalDateTimeline<>(periodeAndel.getFom(), periodeAndel.getTom(), true);
        LocalDateTimeline<Boolean> utenHelg = Hjelpetidslinjer.fjernHelger(tidslinje);
        return utenHelg.stream()
            .map(segment -> new PeriodeAndel(new Periode(segment.getFom(), segment.getTom()), periodeAndel.getVarighetPerDag()))
            .toList();
    }

    private KravDokumentType utledKravDokumentType(Inntektsmelding im) {

        boolean erRefusjon;
        if (zeroRefErKrav) {
            erRefusjon = im.getRefusjonBeløpPerMnd() != null;
        } else {
            erRefusjon = im.getRefusjonBeløpPerMnd() != null && im.getRefusjonBeløpPerMnd().compareTo(Beløp.ZERO) > 0;
        }

        if (erRefusjon) {
            return KravDokumentType.INNTEKTSMELDING;
        } else {
            return KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV;
        }
    }

}
