package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.uttak.FastsattUttakDto;
import no.nav.k9.sak.kontrakt.uttak.OppgittUttakDto;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakAktivitetPeriodeDto;

@Dependent
public class MapUttak {

    private UttakRepository uttakRepository;

    MapUttak() {
        // for proxy
    }

    @Inject
    public MapUttak(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    public OppgittUttakDto mapOppgittUttak(UUID behandlingId) {

        var grunnlag = uttakRepository.hentGrunnlag(behandlingId);
        if (grunnlag.isEmpty()) {
            return new OppgittUttakDto(behandlingId, null);
        } else {
            return mapOppgittUttak(behandlingId, grunnlag.get());
        }
    }

    OppgittUttakDto mapOppgittUttak(UUID behandlingId, UttakGrunnlag grunnlag) {
        var uttak = grunnlag.getOppgittUttak();
        var uttakPerioder = mapUttakPerioder(uttak);
        var res = new OppgittUttakDto(behandlingId, uttakPerioder);
        return res;
    }

    public FastsattUttakDto mapFastsattUttak(UUID behandlingId) {
        var grunnlag = uttakRepository.hentGrunnlag(behandlingId);
        if (grunnlag.isEmpty()) {
            return new FastsattUttakDto(behandlingId, null);
        } else {
            return mapFastsattUttak(behandlingId, grunnlag.get());
        }
    }

    FastsattUttakDto mapFastsattUttak(UUID behandlingId, UttakGrunnlag uttakGrunnlag) {
        var fastsatt = uttakGrunnlag.getFastsattUttak();
        var uttakPerioder = mapUttakPerioder(fastsatt);
        var res = new FastsattUttakDto(behandlingId, uttakPerioder);
        return res;
    }

    private List<UttakAktivitetPeriodeDto> mapUttakPerioder(UttakAktivitet akt) {
        if (akt == null || akt.getPerioder() == null || akt.getPerioder().isEmpty()) {
            return Collections.emptyList();
        }

        List<UttakAktivitetPeriodeDto> result = new ArrayList<>();
        for (var ut : akt.getPerioder()) {
            var periode = new Periode(ut.getPeriode().getFomDato(), ut.getPeriode().getTomDato());
            result.add(new UttakAktivitetPeriodeDto(periode, ut.getAktivitetType()));
        }
        Collections.sort(result);
        return List.copyOf(result);
    }
}
