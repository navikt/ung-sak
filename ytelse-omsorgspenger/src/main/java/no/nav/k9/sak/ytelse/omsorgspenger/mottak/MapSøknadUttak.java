package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.math.BigDecimal;
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
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende;
import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;

class MapSøknadUttak {
    private final OmsorgspengerUtbetaling søknad;

    MapSøknadUttak(OmsorgspengerUtbetaling søknad) {
        this.søknad = søknad;
    }

    UttakGrunnlag lagGrunnlag(Long behandlingId, Set<UttakAktivitetPeriode> perioder, Søker søker) {
        var søknadsperioder = mapSøknadsperioder(søknad.getFraværsperioder());

        var snAktiviteter = Optional.ofNullable(søknad.getAktivitet().getSelvstendigNæringsdrivende())
            .orElse(Collections.emptyList());
        // TODO: Frilans, Arbeistaker

        // Eksisterende uttakaktivitetsperioder
        var uttakPerioder = perioder.stream()
            .map(uttakPeriode -> new UttakAktivitetPeriode(uttakPeriode.getAktivitetType(), uttakPeriode.getPeriode().getFomDato(), uttakPeriode.getPeriode().getTomDato()))
            .collect(Collectors.toList());

        // TODO: Lag filteringslogikk slik at vi kun ser på oppgitt opptjeningsperiode som motsvarer selve søknadsperioden
        // For enkelthets skyld brukes kun første fraværsperiode
        var fraværPeriode = søknad.getFraværsperioder().stream().findFirst().orElseThrow();

        // Nye uttaksaktivitetsperioder - SN
        for (SelvstendigNæringsdrivende sn : snAktiviteter) {
            for (var periode : sn.perioder.keySet()) {
                InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her, tolker om til InternArbeidsforholdRef.nullRef() ved fastsette uttak.
                var arbeidsgiver = sn.organisasjonsnummer != null
                    ? Arbeidsgiver.virksomhet(sn.organisasjonsnummer.verdi)
                    : (søker.norskIdentitetsnummer != null
                    ? Arbeidsgiver.fra(new AktørId(søker.norskIdentitetsnummer.verdi))
                    : null);
                // TODO: Blir det riktig å legge på arbeidsforhold slik for SELVSTENDIG_NÆRINGSDRIVENDE? Eller bare for ARBEIDSTAKER?
                UttakAktivitetPeriode uttakAktivitetPeriode = new UttakAktivitetPeriode(periode.getFraOgMed(), periode.getTilOgMed(), UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver, arbeidsforholdRef,
                    fraværPeriode.getDuration(), BigDecimal.valueOf(100L));
                uttakPerioder.add(uttakAktivitetPeriode);
            }
        }

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
