package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
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
import no.nav.k9.sak.kontrakt.omsorg.BarnRelasjon;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.BeredskapOgNattevåkOppdaterer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.Unntaksperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.personopplysninger.Barn;
import no.nav.k9.søknad.felles.personopplysninger.Bosteder;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap;
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk;
import no.nav.k9.søknad.ytelse.psb.v1.Omsorg;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;

@Dependent
class SøknadOversetter {

    private SøknadRepository søknadRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private MedlemskapRepository medlemskapRepository;
    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;

    SøknadOversetter() {
        // for CDI proxy
    }

    @Inject
    SøknadOversetter(BehandlingRepositoryProvider repositoryProvider,
                     SøknadsperiodeRepository søknadsperiodeRepository,
                     UttakRepository uttakRepository,
                     UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                     TpsTjeneste tpsTjeneste,
                     OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository,
                     UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.tpsTjeneste = tpsTjeneste;
        this.omsorgenForGrunnlagRepository = omsorgenForGrunnlagRepository;
        this.unntakEtablertTilsynGrunnlagRepository = unntakEtablertTilsynGrunnlagRepository;
    }

    void persister(Søknad søknad, JournalpostId journalpostId, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();

        PleiepengerSyktBarn ytelse = søknad.getYtelse();
        var maksSøknadsperiode = ytelse.getSøknadsperiode();
        final List<Periode> søknadsperioder = hentAlleSøknadsperioder(ytelse);

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
            .medJournalpostId(journalpostId)
            .medSøknadId(søknad.getSøknadId() == null ? null : søknad.getSøknadId().getId())
            .medSøknadsdato(maksSøknadsperiode.getFraOgMed())
            .medSpråkkode(getSpraakValg(søknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);

        lagreSøknadsperioder(søknadsperioder, journalpostId, behandlingId);

        // Utgår for K9-ytelsene?
        // .medBegrunnelseForSenInnsending(wrapper.getBegrunnelseForSenSoeknad())
        // .medTilleggsopplysninger(wrapper.getTilleggsopplysninger())

        // TODO etter18feb: lagreOpptjeningForSnOgFl(ytelse.getArbeidAktivitet());

        lagreBeredskapOgNattevåk(søknad, behandlingId);

        // TODO: Hvorfor er getBosteder() noe annet enn getUtenlandsopphold ??
        lagreMedlemskapinfo(ytelse.getBosteder(), behandlingId, mottattDato);

        lagrePleietrengende(fagsakId, ytelse.getBarn());

        lagreUttakOgPerioder(søknad, maksSøknadsperiode, journalpostId, behandlingId, fagsakId);

        lagreOmsorg(ytelse.getOmsorg(), søknadsperioder, behandling);
    }

    private List<Periode> hentAlleSøknadsperioder(PleiepengerSyktBarn ytelse) {
        final LocalDateTimeline<Boolean> kompletteSøknadsperioderTidslinje = tilTidslinje(ytelse.getSøknadsperiodeList());
        final LocalDateTimeline<Boolean> endringssøknadsperioderTidslinje = tilTidslinje(ytelse.getEndringsperiode());
        final LocalDateTimeline<Boolean> søknadsperioder = kompletteSøknadsperioderTidslinje.union(endringssøknadsperioderTidslinje, StandardCombinators::coalesceLeftHandSide).compress();
        return søknadsperioder.stream().map(s -> new Periode(s.getFom(), s.getTom())).collect(Collectors.toList());
    }

    private void lagreSøknadsperioder(List<Periode> søknadsperioder, JournalpostId journalpostId, Long behandlingId) {
        final Søknadsperiode[] søknadsperioderArray = søknadsperioder.stream()
                .map(s -> new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFraOgMed(), s.getTilOgMed())))
                .toArray(Søknadsperiode[]::new);
        søknadsperiodeRepository.lagre(behandlingId, new Søknadsperioder(journalpostId, søknadsperioderArray));
    }

    private LocalDateTimeline<Boolean> tilTidslinje(List<Periode> perioder) {
        return new LocalDateTimeline<>(
                perioder.stream()
                .map(p -> new LocalDateSegment<>(p.getFraOgMed(), p.getTilOgMed(), Boolean.TRUE))
                .collect(Collectors.toList())
                ).compress();
    }

    private void lagreBeredskapOgNattevåk(Søknad søknad, final Long behandlingId) {
        var ytelse = (PleiepengerSyktBarn) søknad.getYtelse();

        if (ytelse.getBeredskap().getPerioder().isEmpty()
                && (ytelse.getBeredskap().getPerioderSomSkalSlettes() == null || ytelse.getBeredskap().getPerioderSomSkalSlettes().isEmpty())
                && ytelse.getNattevåk().getPerioder().isEmpty()
                && (ytelse.getNattevåk().getPerioderSomSkalSlettes() == null || ytelse.getNattevåk().getPerioderSomSkalSlettes().isEmpty())) {
            // Ingen endringer.
            return;
        }


        var pleietrengendePersonIdent = søknad.getYtelse().getPleietrengende().getPersonIdent();
        var søkerPersonIdent = søknad.getSøker().getPersonIdent();
        var pleietrengendeAktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(pleietrengendePersonIdent.getVerdi())).orElseThrow();
        var søkerAktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(søkerPersonIdent.getVerdi())).orElseThrow();

        var eksisterendeGrunnlag = unntakEtablertTilsynGrunnlagRepository.hentHvisEksistererUnntakPleietrengende(pleietrengendeAktørId);
        var eksisterendeBeredskap = eksisterendeGrunnlag
            .map(UnntakEtablertTilsynForPleietrengende::getBeredskap)
            .orElse(null);
        var eksisterendeNattevåk = eksisterendeGrunnlag
            .map(UnntakEtablertTilsynForPleietrengende::getNattevåk)
            .orElse(null);

        /*
         * TODO: Data bør ikke mappes direkte inn her, men heller legges i tabeller der
         *       informasjonen er knyttet til kravdokument. Ved behandling av steget
         *       kan man da utlede hva som gjelder basert på rekkefølge osv.
         */
        var unntakEtablertTilsynBeredskap =
            tilUnntakEtablertTilsynForPleietrengende(
                eksisterendeBeredskap,
                søknad.getMottattDato().toLocalDate(),
                søkerAktørId,
                behandlingId,
                ytelse.getBeredskap());
        var unntakEtablertTilsynNattevåk =
            tilUnntakEtablertTilsynForPleietrengende(
                eksisterendeNattevåk,
                søknad.getMottattDato().toLocalDate(),
                søkerAktørId,
                behandlingId,
                ytelse.getNattevåk());

        var unntakEtablertTilsynForPleietrengende = new UnntakEtablertTilsynForPleietrengende(
            pleietrengendeAktørId,
            unntakEtablertTilsynBeredskap,
            unntakEtablertTilsynNattevåk
        );
        unntakEtablertTilsynGrunnlagRepository.lagre(behandlingId, unntakEtablertTilsynForPleietrengende);
    }

    private static UnntakEtablertTilsyn tilUnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, Long kildeBehandlingId, Beredskap beredskap) {
        var nyeUnntakBeredskap = new ArrayList<Unntaksperiode>();
        beredskap.getPerioder().forEach( (key,value) ->
            nyeUnntakBeredskap.add(new Unntaksperiode(key.getFraOgMed(), key.getTilOgMed(), value.getTilleggsinformasjon(), Resultat.IKKE_VURDERT))
        );
        var unntakSomSkalSlettes = new ArrayList<no.nav.k9.sak.typer.Periode>();
        beredskap.getPerioderSomSkalSlettes().forEach( (key,value) ->
            unntakSomSkalSlettes.add(new no.nav.k9.sak.typer.Periode(key.getFraOgMed(), key.getTilOgMed()))
        );
        return BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraSøknad(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, kildeBehandlingId, nyeUnntakBeredskap, unntakSomSkalSlettes);
    }

    private static UnntakEtablertTilsyn tilUnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, Long kildeBehandlingId, Nattevåk nattevåk) {
        var nyeUnntakNattevåk = new ArrayList<Unntaksperiode>();
        nattevåk.getPerioder().forEach( (key,value) ->
            nyeUnntakNattevåk.add(new Unntaksperiode(key.getFraOgMed(), key.getTilOgMed(), value.getTilleggsinformasjon(), Resultat.IKKE_VURDERT))
        );
        var unntakSomSkalSlettes = new ArrayList<no.nav.k9.sak.typer.Periode>();
        nattevåk.getPerioderSomSkalSlettes().forEach( (key,value) ->
            unntakSomSkalSlettes.add(new no.nav.k9.sak.typer.Periode(key.getFraOgMed(), key.getTilOgMed()))
        );
        return BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraSøknad(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, kildeBehandlingId, nyeUnntakNattevåk, unntakSomSkalSlettes);
    }

    private void lagreOmsorg(Omsorg omsorg, List<Periode> søknadsperioder, Behandling behandling) {
        for (var periode : søknadsperioder) {
            final OmsorgenForPeriode omsorgForPeriode = OmsorgenForPeriode.nyPeriodeFraSøker(
                DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()),
                BarnRelasjon.of(omsorg.getRelasjonTilBarnet().isPresent() ? omsorg.getRelasjonTilBarnet().get().getRolle() : null),
                omsorg.getBeskrivelseAvOmsorgsrollen().isPresent() ? omsorg.getBeskrivelseAvOmsorgsrollen().get() : null);
            omsorgenForGrunnlagRepository.lagre(behandling.getId(), omsorgForPeriode);
        }
    }

    private void lagreUttakOgPerioder(Søknad soknad, Periode maksSøknadsperiode, JournalpostId journalpostId, final Long behandlingId, Long fagsakId) {
        // TODO etter18feb: LovbestemtFerie

        // TODO 18feb: Arbeidstid
        // TODO etter18feb: UttakPeriodeInfo
        var perioderFraSøknad = new MapSøknadUttakPerioder(soknad, journalpostId).getPerioderFraSøknad();
        uttakPerioderGrunnlagRepository.lagre(behandlingId, perioderFraSøknad);

        fagsakRepository.utvidPeriode(fagsakId, maksSøknadsperiode.getFraOgMed(), maksSøknadsperiode.getTilOgMed());
    }

    private void lagrePleietrengende(Long fagsakId, Barn barn) {
        final var norskIdentitetsnummer = barn.getPersonIdent();
        if (norskIdentitetsnummer != null) {
            final var aktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(norskIdentitetsnummer.getVerdi())).orElseThrow();
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
                    .medLand(finnLandkode(opphold.getLand().getLandkode()))
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
            return Språkkode.fraKode(spraak.getKode().toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }
}
