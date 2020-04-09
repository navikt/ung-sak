package no.nav.k9.sak.ytelse.frisinn.mottak;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.søknad.felles.Språk;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;

@Dependent
class LagreSøknad {

    private SøknadRepository søknadRepository;
    private UttakRepository uttakRepository;
    @SuppressWarnings("unused")
    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;
    private LagreOppgittOpptjening lagreOppgittOpptjening;

    LagreSøknad() {
        // for CDI proxy
    }

    @Inject
    LagreSøknad(FagsakRepository fagsakRepository,
                UttakRepository uttakRepository,
                SøknadRepository søknadRepository,
                TpsTjeneste tpsTjeneste,
                LagreOppgittOpptjening lagreOppgittOpptjening) {
        this.fagsakRepository = fagsakRepository;
        this.søknadRepository = søknadRepository;
        this.uttakRepository = uttakRepository;
        this.tpsTjeneste = tpsTjeneste;
        this.lagreOppgittOpptjening = lagreOppgittOpptjening;
    }

    void persister(FrisinnSøknad soknad, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();

        final boolean elektroniskSøknad = false;
        var søknadBuilder = new SøknadEntitet.Builder()
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(soknad.getMottattDato().toLocalDate())
            .medErEndringssøknad(false) // støtter ikke endringssønader p.t.
            .medSøknadsdato(soknad.getMottattDato().toLocalDate())
            .medSpråkkode(getSpraakValg(soknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);

        lagrePerioder(soknad, behandlingId, fagsakId);
        
        lagreOppgittOpptjening.lagreOpptjening(behandling, soknad.getInntekter(), soknad.getMottattDato());

    }

    private void lagrePerioder(FrisinnSøknad soknad, final Long behandlingId, Long fagsakId) {
        var mapUttakGrunnlag = new MapSøknadUttak(soknad).lagGrunnlag(behandlingId);
        uttakRepository.lagreOgFlushNyttGrunnlag(behandlingId, mapUttakGrunnlag);

        var maksPeriode = mapUttakGrunnlag.getOppgittSøknadsperioder().getMaksPeriode();
        fagsakRepository.utvidPeriode(fagsakId, maksPeriode.getFomDato(), maksPeriode.getTomDato());
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.dto.toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

}
