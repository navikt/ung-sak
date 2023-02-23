package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class InntektsmeldingSøktePerioderMapper {

    private boolean zeroRefErKrav;

    @Inject
    public InntektsmeldingSøktePerioderMapper(@KonfigVerdi(value = "OMS_ZERO_REFUSJON_ER_KRAV", defaultVerdi = "false") boolean zeroRefErKrav) {
        this.zeroRefErKrav = zeroRefErKrav;
    }

    public InntektsmeldingSøktePerioderMapper() {
    }

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> mapTilSøktePerioder(Set<Inntektsmelding> inntektsmeldinger) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> result = new HashMap<>();
        inntektsmeldinger.forEach(it -> mapTilSøktePerioder(result, it));
        return result;
    }

    private void mapTilSøktePerioder(Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> result, no.nav.k9.sak.domene.iay.modell.Inntektsmelding it) {
        result.put(new KravDokument(it.getJournalpostId(), it.getInnsendingstidspunkt(), utledKravDokumentType(it)),
            it.getOppgittFravær()
                .stream()
                .map(pa -> new OppgittFraværPeriode(it.getJournalpostId(), pa.getFom(), pa.getTom(), UttakArbeidType.ARBEIDSTAKER, it.getArbeidsgiver(), it.getArbeidsforholdRef(), pa.getVarighetPerDag(), FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT))
                .map(op -> new SøktPeriode<>(op.getPeriode(), op.getAktivitetType(), op.getArbeidsgiver(), op.getArbeidsforholdRef(), op))
                .collect(Collectors.toList()));
    }

    private KravDokumentType utledKravDokumentType(Inntektsmelding im) {

        var erRefusjon = false;
        if (zeroRefErKrav) {
            erRefusjon = im.getRefusjonBeløpPerMnd() != null;
        } else {
            erRefusjon = im.getRefusjonBeløpPerMnd() != null && im.getRefusjonBeløpPerMnd().compareTo(Beløp.ZERO) > 0;

        }

        if (erRefusjon) {
            return KravDokumentType.INNTEKTSMELDING;
        }
        else {
            return KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV;
        }
    }

}
