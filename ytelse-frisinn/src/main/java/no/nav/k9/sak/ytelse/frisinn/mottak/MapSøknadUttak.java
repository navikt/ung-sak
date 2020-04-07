package no.nav.k9.sak.ytelse.frisinn.mottak;

import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;

class MapSøknadUttak {
    private FrisinnSøknad søknad;

    MapSøknadUttak(FrisinnSøknad søknad) {
        this.søknad = søknad;
    }

    UttakGrunnlag getUttakGrunnlag(Long behandlingId) {
        var søknadsperioder = mapSøknadsperiode(søknad);
        return new UttakGrunnlag(behandlingId, null, søknadsperioder, null, null);
    }

    private static Søknadsperioder mapSøknadsperiode(FrisinnSøknad søknad) {
        return new Søknadsperioder(new Søknadsperiode(søknad.getSøknadsperiode().fraOgMed, søknad.getSøknadsperiode().tilOgMed));
    }

}
