package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.util.ArrayList;
import java.util.List;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;
import no.nav.k9.søknad.frisinn.Inntekter;

class MapSøknadUttak {
    private FrisinnSøknad søknad;

    MapSøknadUttak(FrisinnSøknad søknad) {
        this.søknad = søknad;
    }

    UttakGrunnlag lagGrunnlag(Long behandlingId) {
        var søknadsperioder = mapSøknadsperiode(søknad);
        Inntekter inntekter = søknad.getInntekter();
        var frilanser = inntekter.getFrilanser();
        var selvstendig = inntekter.getSelvstendig();
        List<UttakAktivitetPeriode> uttakPerioder = new ArrayList<>();
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
