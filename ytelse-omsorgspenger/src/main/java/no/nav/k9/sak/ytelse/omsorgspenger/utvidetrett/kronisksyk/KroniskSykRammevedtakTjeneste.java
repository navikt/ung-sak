package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakResponse;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

@ApplicationScoped
@Default
public class KroniskSykRammevedtakTjeneste {
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private SøknadRepository søknadRepository;

    @Inject
    public KroniskSykRammevedtakTjeneste(ÅrskvantumTjeneste årskvantumTjeneste, BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste, PersoninfoAdapter personinfoAdapter, SøknadRepository søknadRepository) {
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.søknadRepository = søknadRepository;
    }

    public KroniskSykRammevedtakTjeneste() {
        // for CDI
    }

    public RammevedtakResponse hentRammevedtak(BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingsprosessTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        PersonIdent personIdent = personinfoAdapter.hentIdentForAktørId(behandling.getAktørId())
            .orElseGet(() -> { throw new IllegalArgumentException("todo: finn på en feilmelding"); });

        SøknadEntitet søknad = søknadRepository.hentSøknad(behandling);
        if(søknad == null) {
            throw new IllegalArgumentException("todo: finn på en feilmelding");
        }
        DatoIntervallEntitet søknadsperiode = søknad.getSøknadsperiode();

        return årskvantumTjeneste.hentRammevedtak(personIdent, new LukketPeriode(søknadsperiode.getFomDato(), søknadsperiode.getTomDato()));
    }

}
