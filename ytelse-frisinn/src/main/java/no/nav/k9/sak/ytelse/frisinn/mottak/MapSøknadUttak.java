package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;
import no.nav.k9.søknad.frisinn.Inntekter;

class MapSøknadUttak {
    private final FrisinnSøknad søknad;

    MapSøknadUttak(FrisinnSøknad søknad) {
        this.søknad = søknad;
    }

    UttakGrunnlag lagGrunnlag(Long behandlingId, Set<UttakAktivitetPeriode> perioder) {
        var søknadsperioder = mapSøknadsperiode(søknad);
        Inntekter inntekter = søknad.getInntekter();
        var frilanser = inntekter.getFrilanser();
        var selvstendig = inntekter.getSelvstendig();

        List<UttakAktivitetPeriode> uttakPerioder = perioder
                .stream()
                .map(uttakAktivitetPeriode -> new UttakAktivitetPeriode(uttakAktivitetPeriode.getAktivitetType(), uttakAktivitetPeriode.getPeriode().getFomDato(), uttakAktivitetPeriode.getPeriode().getTomDato()))
                .collect(Collectors.toList());

        if (frilanser != null && frilanser.getSøkerKompensasjon()) {
            var periode = frilanser.getMaksSøknadsperiode();
            uttakPerioder.add(new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, periode.getFraOgMed(), periode.getTilOgMed()));
        }
        if (selvstendig != null && selvstendig.getSøkerKompensasjon()) {
            var periode = selvstendig.getMaksSøknadsperiode();
            uttakPerioder.add(new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, periode.getFraOgMed(), periode.getTilOgMed()));
        }

        var oppgittUttak = new UttakAktivitet(uttakPerioder);
        var fastsattUttak = oppgittUttak;
        return new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, søknadsperioder);
    }

    private static Søknadsperioder mapSøknadsperiode(FrisinnSøknad søknad) {
        return new Søknadsperioder(new Søknadsperiode(søknad.getSøknadsperiode().getFraOgMed(), søknad.getSøknadsperiode().getTilOgMed()));
    }

}
