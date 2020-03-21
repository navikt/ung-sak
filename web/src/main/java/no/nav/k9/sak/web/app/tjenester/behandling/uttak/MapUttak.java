package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.uttak.FastsattUttakDto;
import no.nav.k9.sak.kontrakt.uttak.OppgittUttakDto;
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
            return new OppgittUttakDto(behandlingId);
        } else {
            return mapOppgittUttak(behandlingId, grunnlag.get());
        }
    }

    OppgittUttakDto mapOppgittUttak(UUID behandlingId, UttakGrunnlag grunnlag) {
        var res = new OppgittUttakDto(behandlingId);
        var søknadsperioder = grunnlag.getOppgittSøknadsperioder();
        var ferie = grunnlag.getOppgittFerie();
        var tilsynsordning = grunnlag.getOppgittTilsynsordning();
        var uttak = grunnlag.getOppgittUttak();

        return res;
    }

    public FastsattUttakDto mapFastsattUttak(UUID behandlingId) {
        var grunnlag = uttakRepository.hentGrunnlag(behandlingId);
        if (grunnlag.isEmpty()) {
            return new FastsattUttakDto(behandlingId);
        } else {
            return mapFastsattUttak(behandlingId, grunnlag.get());
        }
    }

    FastsattUttakDto mapFastsattUttak(UUID behandlingId, UttakGrunnlag uttakGrunnlag) {
        var res = new FastsattUttakDto(behandlingId);
        var fastsatt = uttakGrunnlag.getFastsattUttak();
        var uttakPerioder = mapUttakPerioder(fastsatt);
        return res;
    }

    private List<UttakAktivitetPeriodeDto> mapUttakPerioder(UttakAktivitet fastsatt) {
        // TODO Auto-generated method stub
        return null;
    }
}
