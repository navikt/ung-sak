package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;

class MapSøknadUttak {
    private final OmsorgspengerUtbetaling søknad;

    MapSøknadUttak(OmsorgspengerUtbetaling søknad) {
        this.søknad = søknad;
    }

    UttakGrunnlag lagGrunnlag(Long behandlingId, Set<UttakAktivitetPeriode> perioder) {
        var søknadsperioder = mapSøknadsperioder(søknad.getFraværsperioder());

        var snAktiviteter = Optional.ofNullable(søknad.getAktivitet().getSelvstendigNæringsdrivende())
            .orElse(Collections.emptyList());
        var atAktiviteter = Optional.ofNullable(søknad.getAktivitet().getArbeidstaker())
            .orElse(Collections.emptyList());
        // TODO: Frilans

        // Eksisterende uttakaktivitetsperioder
        var uttakPerioder = perioder.stream()
            .map(uttakPeriode -> new UttakAktivitetPeriode(uttakPeriode.getAktivitetType(), uttakPeriode.getPeriode().getFomDato(), uttakPeriode.getPeriode().getTomDato()))
            .collect(Collectors.toList());
        // Nye uttaksaktivitetsperioder
        snAktiviteter.stream()
            .map(sn -> sn.perioder.keySet())
            .flatMap(Collection::stream)
            .map(periode -> new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, periode.getFraOgMed(), periode.getTilOgMed()))
            .forEach(uttakPeriode ->uttakPerioder.add(uttakPeriode));
        atAktiviteter.stream()
            .map(at -> at.perioder.keySet())
            .flatMap(Collection::stream)
            .map(periode -> new UttakAktivitetPeriode(UttakArbeidType.ARBEIDSTAKER, periode.getFraOgMed(), periode.getTilOgMed()))
            .forEach(uttakPeriode ->uttakPerioder.add(uttakPeriode));

        var oppgittUttak = new UttakAktivitet(uttakPerioder);
        var fastsattUttak = oppgittUttak;
        return new UttakGrunnlag(behandlingId, oppgittUttak, fastsattUttak, søknadsperioder);
    }

    private Søknadsperioder mapSøknadsperioder(List<FraværPeriode> input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        var mappedPerioder = input.stream()
            .map(fraværPeriode -> new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(fraværPeriode.getPeriode().getFraOgMed(), fraværPeriode.getPeriode().getTilOgMed())))
            .collect(Collectors.toSet());

        return new Søknadsperioder(mappedPerioder);
    }

}
