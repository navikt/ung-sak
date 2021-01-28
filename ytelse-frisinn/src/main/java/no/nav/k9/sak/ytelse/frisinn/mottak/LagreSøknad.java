package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;

@Dependent
class LagreSøknad {

    private SøknadRepository søknadRepository;
    private UttakRepository uttakRepository;
    @SuppressWarnings("unused")
    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    LagreSøknad() {
        // for CDI proxy
    }

    @Inject
    LagreSøknad(FagsakRepository fagsakRepository,
                UttakRepository uttakRepository,
                SøknadRepository søknadRepository,
                TpsTjeneste tpsTjeneste,
                BehandlingRepository behandlingRepository) {
        this.fagsakRepository = fagsakRepository;
        this.søknadRepository = søknadRepository;
        this.uttakRepository = uttakRepository;
        this.tpsTjeneste = tpsTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    void persister(FrisinnSøknad søknad, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        var søknadsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(søknad.getSøknadsperiode().getFraOgMed(), søknad.getSøknadsperiode().getTilOgMed());

        final boolean elektroniskSøknad = false;
        LocalDate mottattDato = søknad.getMottattDato().withZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(søknadsperiode)
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(mottattDato)
            .medErEndringssøknad(false) // støtter ikke endringssønader p.t.
            .medSøknadsdato(mottattDato)
            .medSpråkkode(getSpraakValg(søknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);

        lagrePerioder(søknad, behandlingId, fagsakId);

    }

    List<Behandling> finnesSøknadForSammePeriode(Long fagsakId, LocalDate fom, LocalDate tom) {
        var behandlinger = søknadRepository.hentBehandlingerMedOverlappendeSøknaderIPeriode(fagsakId, fom, tom);
        return behandlinger;
    }

    private void lagrePerioder(FrisinnSøknad soknad, final Long behandlingId, Long fagsakId) {
        var sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        if (sisteBehandling.isPresent()) {
            UttakAktivitet uttakAktivitet = uttakRepository.hentFastsattUttak(sisteBehandling.get().getId());
            lagreOgUtvidSøkerPeriode(soknad, behandlingId, fagsakId, uttakAktivitet.getPerioder());
        } else {
            lagreOgUtvidSøkerPeriode(soknad, behandlingId, fagsakId, Collections.emptySet());
        }
    }

    private void lagreOgUtvidSøkerPeriode(FrisinnSøknad soknad, Long behandlingId, Long fagsakId, Set<UttakAktivitetPeriode> uttakAktivitetPerioder) {
        var mapUttakGrunnlag = new MapSøknadUttak(soknad).lagGrunnlag(behandlingId, uttakAktivitetPerioder);
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
