package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class InntektsmeldingSøktePerioderMapper {

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> mapTilSøktePerioder(Set<Inntektsmelding> inntektsmeldinger) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> result = new HashMap<>();
        inntektsmeldinger.forEach(it -> mapTilSøktePerioder(result, it));
        return result;
    }

    private void mapTilSøktePerioder(Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> result, no.nav.k9.sak.domene.iay.modell.Inntektsmelding it) {
        result.put(new KravDokument(it.getJournalpostId(), it.getInnsendingstidspunkt(), KravDokumentType.INNTEKTSMELDING),
            it.getOppgittFravær()
                .stream()
                .map(pa -> new OppgittFraværPeriode(pa.getFom(), pa.getTom(), UttakArbeidType.ARBEIDSTAKER, it.getArbeidsgiver(), it.getArbeidsforholdRef(), pa.getVarighetPerDag()))
                .map(op -> new SøktPeriode<>(op.getPeriode(), op.getAktivitetType(), op.getArbeidsgiver(), op.getArbeidsforholdRef(), op))
                .collect(Collectors.toList()));
    }
}
