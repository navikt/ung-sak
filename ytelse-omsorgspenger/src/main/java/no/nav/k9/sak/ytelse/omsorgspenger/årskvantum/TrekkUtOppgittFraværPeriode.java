package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristResultat;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.InntektsmeldingSøktePerioderMapper;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.VurderSøknadsfrist;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.KravDokumentFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
class TrekkUtOppgittFraværPeriode {
    private static final Logger log = LoggerFactory.getLogger(TrekkUtOppgittFraværPeriode.class);

    private InntektsmeldingSøktePerioderMapper inntektsmeldingMapper;
    private VurderSøknadsfrist vurderSøknadsfrist;

    @Inject
    TrekkUtOppgittFraværPeriode(VurderSøknadsfrist vurderSøknadsfrist,
                                InntektsmeldingSøktePerioderMapper inntektsmeldingMapper) {
        this.vurderSøknadsfrist = vurderSøknadsfrist;
        this.inntektsmeldingMapper = inntektsmeldingMapper;
    }

    Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> mapFra(LinkedHashSet<Inntektsmelding> inntektsmeldinger,
                                                                             Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> kravDokumenterMedFravær,
                                                                             Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søkteFraværsperioder = new LinkedHashMap<>();
        var søktePerioder = inntektsmeldingMapper.mapTilSøktePerioder(inntektsmeldinger);
        søkteFraværsperioder.putAll(søktePerioder);
        søkteFraværsperioder.putAll(kravDokumenterMedFravær);

        var vurdertePerioder = vurderSøknadsfrist.vurderSøknadsfrist(søkteFraværsperioder, avklartSøknadsfristResultat);

        var antallIM = vurdertePerioder.keySet().stream().filter(type -> KravDokumentType.INNTEKTSMELDING.equals(type.getType())).count();
        var antallSøknader = vurdertePerioder.keySet().stream().filter(type -> KravDokumentType.SØKNAD.equals(type.getType())).count();
        log.info("Fant {} inntektsmeldinger og {} søknader knyttet til behandlingen:", antallIM, antallSøknader);

        return Collections.unmodifiableMap(vurdertePerioder);
    }

    List<WrappedOppgittFraværPeriode> trekkUtFravær(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværPerKravdokument) {
        return new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(fraværPerKravdokument);
    }

}
