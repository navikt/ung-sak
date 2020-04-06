package no.nav.k9.sak.ytelse.frisinn.mottak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.søknad.felles.Språk;
import no.nav.k9.søknad.pleiepengerbarn.FrisinnSøknad;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
class SøknadOversetter {

    private SøknadRepository søknadRepository;
    private UttakRepository uttakRepository;
    @SuppressWarnings("unused")
    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;

    SøknadOversetter() {
        // for CDI proxy
    }

    @Inject
    SøknadOversetter(BehandlingRepositoryProvider repositoryProvider,
                                           UttakRepository uttakRepository,
                                           TpsTjeneste tpsTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.uttakRepository = uttakRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    void persister(FrisinnSøknad soknad, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();

        // TODO:
        final boolean elektroniskSøknad = false;
        var søknadBuilder = new SøknadEntitet.Builder()
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(soknad.getMottattDato().toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(soknad.getMottattDato().toLocalDate()) // TODO: Hva er dette? Dette feltet er datoen det gjelder fra for FP-endringssøknader.
            .medSpråkkode(getSpraakValg(soknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);

        lagreUttakOgPerioder(soknad, behandlingId, fagsakId);
    }

    private void lagreUttakOgPerioder(FrisinnSøknad soknad, final Long behandlingId, Long fagsakId) {
        var mapUttakGrunnlag = new MapSøknadUttak(soknad).getUttakGrunnlag(behandlingId);
        uttakRepository.lagreOgFlushNyttGrunnlag(behandlingId, mapUttakGrunnlag);

        var maksPeriode = mapUttakGrunnlag.getOppgittUttak().getMaksPeriode();

        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var eksisterendeFom = fagsak.getPeriode().getFomDato();
        var eksisterendeTom = fagsak.getPeriode().getTomDato();
        var oppdatertFom = eksisterendeFom.isBefore(maksPeriode.getFomDato()) && !Tid.TIDENES_BEGYNNELSE.equals(eksisterendeFom) ? eksisterendeFom : maksPeriode.getFomDato();
        var oppdatertTom = eksisterendeTom.isAfter(maksPeriode.getTomDato()) && !Tid.TIDENES_ENDE.equals(eksisterendeTom) ? eksisterendeTom : maksPeriode.getTomDato();

        fagsakRepository.oppdaterPeriode(fagsakId, oppdatertFom, oppdatertTom);
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.dto.toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }
}
