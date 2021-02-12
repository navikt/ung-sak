package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.LocalDate;
import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.personopplysninger.Barn;
import no.nav.k9.søknad.felles.personopplysninger.Bosteder;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;
import no.nav.vedtak.konfig.Tid;

@Dependent
class SøknadOversetter {

    private SøknadRepository søknadRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private MedlemskapRepository medlemskapRepository;
    private UttakRepository uttakRepository;
    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;

    SøknadOversetter() {
        // for CDI proxy
    }

    @Inject
    SøknadOversetter(BehandlingRepositoryProvider repositoryProvider,
                     SøknadsperiodeRepository søknadsperiodeRepository,
                     UttakRepository uttakRepository,
                     TpsTjeneste tpsTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.uttakRepository = uttakRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    void persister(Søknad søknad, JournalpostId journalpostId, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();

        PleiepengerSyktBarn ytelse = søknad.getYtelse();
        var maksSøknadsperiode = ytelse.getSøknadsperiode();

        // TODO: Stopp barn som mangler norskIdentitetsnummer i k9-punsj ... eller støtt fødselsdato her?

        // TODO etter18feb: Fjern denne fra entitet og DB:
        final boolean elektroniskSøknad = false;

        LocalDate mottattDato = søknad.getMottattDato().toLocalDate();

        // TODO: Hvis vi skal beholde SøknadEntitet trenger vi å lagre SøknadID og sikre idempotens med denne.

        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(maksSøknadsperiode.getFraOgMed(), maksSøknadsperiode.getTilOgMed()))
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(mottattDato)
            .medErEndringssøknad(false) // TODO: Håndtere endringssøknad. "false" betyr at vi krever IMer.
            .medSøknadsdato(maksSøknadsperiode.getFraOgMed())
            .medSpråkkode(getSpraakValg(søknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);

        var søknadsperiode = new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(maksSøknadsperiode.getFraOgMed(), maksSøknadsperiode.getTilOgMed()));
        søknadsperiodeRepository.lagre(behandlingId, new Søknadsperioder(journalpostId, søknadsperiode));

        // Utgår for K9-ytelsene?
        // .medBegrunnelseForSenInnsending(wrapper.getBegrunnelseForSenSoeknad())
        // .medTilleggsopplysninger(wrapper.getTilleggsopplysninger())

        // TODO etter18feb: lagreOpptjeningForSnOgFl(ytelse.getArbeidAktivitet());

        // TODO etter18feb: Beredskap, nattevåk og tilsynsordning

        // TODO: Hvorfor er getBosteder() noe annet enn getUtenlandsopphold ??
        lagreMedlemskapinfo(ytelse.getBosteder(), behandlingId, mottattDato);

        lagrePleietrengende(fagsakId, ytelse.getBarn());

        lagreUttakOgPerioder(søknad, behandlingId, fagsakId);

        // TODO etter18feb: Omsorg
    }

    private void lagreUttakOgPerioder(Søknad soknad, final Long behandlingId, Long fagsakId) {
        // TODO etter18feb: LovbestemtFerie

        // TODO 18feb: Arbeidstid
        // TODO etter18feb: UttakPeriodeInfo

        var mapUttakGrunnlag = new MapSøknadUttak(soknad).getUttakGrunnlag(behandlingId);
        uttakRepository.lagreOgFlushNyttGrunnlag(behandlingId, mapUttakGrunnlag);

        var maksPeriode = mapUttakGrunnlag.getOppgittSøknadsperioder().getMaksPeriode();
        fagsakRepository.utvidPeriode(fagsakId, maksPeriode.getFomDato(), maksPeriode.getTomDato());
    }

    private void lagrePleietrengende(Long fagsakId, Barn barn) {
        final var norskIdentitetsnummer = barn.norskIdentitetsnummer;
        if (norskIdentitetsnummer != null) {
            final var aktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(norskIdentitetsnummer.verdi)).orElseThrow();
            fagsakRepository.oppdaterPleietrengende(fagsakId, aktørId);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void lagreMedlemskapinfo(Bosteder bosteder, Long behandlingId, LocalDate forsendelseMottatt) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);

        // TODO: Hva skal vi ha som "oppholdNå"?
        // Boolean iNorgeVedFoedselstidspunkt = medlemskap.isINorgeVedFoedselstidspunkt();
        // oppgittTilknytningBuilder.medOppholdNå(Boolean.TRUE.equals(iNorgeVedFoedselstidspunkt));

        if (bosteder != null) {
            bosteder.getPerioder().forEach((periode, opphold) -> {
                // TODO: "tidligereOpphold" må fjernes fra database og domeneobjekter. Ved bruk må skjæringstidspunkt spesifikt oppgis.
                // boolean tidligereOpphold = opphold.getPeriode().getFom().isBefore(mottattDato);
                oppgittTilknytningBuilder.leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                    .medLand(finnLandkode(opphold.getLand().landkode))
                    .medPeriode(
                        Objects.requireNonNull(periode.getFraOgMed()),
                        Objects.requireNonNullElse(periode.getTilOgMed(), Tid.TIDENES_ENDE))
                    // .erTidligereOpphold(tidligereOpphold)
                    .build());
            });
        }
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
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
