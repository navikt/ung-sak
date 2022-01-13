package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.personopplysninger.Bosteder;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap;
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk;
import no.nav.k9.søknad.ytelse.psb.v1.Omsorg;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;

@Dependent
public class SøknadPersisterer {

    private SøknadRepository søknadRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private MedlemskapRepository medlemskapRepository;
    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private UnntakEtablertTilsynGrunnlagRepository unntakEtablertTilsynGrunnlagRepository;

    @Inject
    SøknadPersisterer(BehandlingRepositoryProvider repositoryProvider,
                      SøknadsperiodeRepository søknadsperiodeRepository,
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

    private static UnntakEtablertTilsyn tilUnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, Long kildeBehandlingId, Beredskap beredskap) {
        var nyeUnntakBeredskap = new ArrayList<Unntaksperiode>();
        beredskap.getPerioder().forEach((key, value) ->
            nyeUnntakBeredskap.add(new Unntaksperiode(key.getFraOgMed(), key.getTilOgMed(), value.getTilleggsinformasjon(), Resultat.IKKE_VURDERT))
        );
        var unntakSomSkalSlettes = new ArrayList<no.nav.k9.sak.typer.Periode>();
        beredskap.getPerioderSomSkalSlettes().forEach((key, value) ->
            unntakSomSkalSlettes.add(new no.nav.k9.sak.typer.Periode(key.getFraOgMed(), key.getTilOgMed()))
        );
        return BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraSøknad(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, kildeBehandlingId, nyeUnntakBeredskap, unntakSomSkalSlettes);
    }

    private static UnntakEtablertTilsyn tilUnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, Long kildeBehandlingId, Nattevåk nattevåk) {
        var nyeUnntakNattevåk = new ArrayList<Unntaksperiode>();
        nattevåk.getPerioder().forEach((key, value) ->
            nyeUnntakNattevåk.add(new Unntaksperiode(key.getFraOgMed(), key.getTilOgMed(), value.getTilleggsinformasjon(), Resultat.IKKE_VURDERT))
        );
        var unntakSomSkalSlettes = new ArrayList<no.nav.k9.sak.typer.Periode>();
        nattevåk.getPerioderSomSkalSlettes().forEach((key, value) ->
            unntakSomSkalSlettes.add(new no.nav.k9.sak.typer.Periode(key.getFraOgMed(), key.getTilOgMed()))
        );
        return BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraSøknad(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, kildeBehandlingId, nyeUnntakNattevåk, unntakSomSkalSlettes);
    }

    void lagreBeredskapOgNattevåk(Søknad søknad, final Long behandlingId) {
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

    void lagreOmsorg(Omsorg omsorg, List<Periode> søknadsperioder, Behandling behandling) {
        for (var periode : søknadsperioder) {
            final OmsorgenForPeriode omsorgForPeriode = OmsorgenForPeriode.nyPeriodeFraSøker(
                DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()),
                BarnRelasjon.of(omsorg.getRelasjonTilBarnet().isPresent() ? omsorg.getRelasjonTilBarnet().get().getRolle() : null),
                omsorg.getBeskrivelseAvOmsorgsrollen().isPresent() ? omsorg.getBeskrivelseAvOmsorgsrollen().get() : null);
            omsorgenForGrunnlagRepository.lagre(behandling.getId(), omsorgForPeriode);
        }
    }

    public void lagrePleietrengende(Long fagsakId, no.nav.k9.søknad.felles.type.PersonIdent norskIdentitetsnummer) {
        if (norskIdentitetsnummer != null) {
            final var aktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(norskIdentitetsnummer.getVerdi())).orElseThrow();
            fagsakRepository.oppdaterPleietrengende(fagsakId, aktørId);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void lagreMedlemskapinfo(Bosteder bosteder, Long behandlingId, LocalDate forsendelseMottatt) {
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

    public void lagreSøknadEntitet(Søknad søknad, JournalpostId journalpostId, Long behandlingId, Optional<Periode> maksSøknadsperiode, LocalDate mottattDato) {
        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(maksSøknadsperiode.map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFraOgMed(), it.getTilOgMed())).orElse(null))
            .medElektroniskRegistrert(true)
            .medMottattDato(mottattDato)
            .medErEndringssøknad(false)
            .medJournalpostId(journalpostId)
            .medSøknadId(søknad.getSøknadId() == null ? null : søknad.getSøknadId().getId())
            .medSøknadsdato(maksSøknadsperiode.map(Periode::getFraOgMed).orElse(mottattDato))
            .medSpråkkode(getSpraakValg(søknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);
    }

    public void lagreSøknadsperioder(List<Periode> søknadsperioder, List<Periode> trekkKravPerioder, JournalpostId journalpostId, Long behandlingId) {
        final List<Søknadsperiode> søknadsperiodeliste = new ArrayList<>();
        søknadsperioder.stream()
            .map(s -> new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFraOgMed(), s.getTilOgMed())))
            .forEach(søknadsperiodeliste::add);
        trekkKravPerioder.stream()
            .map(s -> new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFraOgMed(), s.getTilOgMed()), true))
            .forEach(søknadsperiodeliste::add);

        søknadsperiodeRepository.lagre(behandlingId, new Søknadsperioder(journalpostId, søknadsperiodeliste.toArray(new Søknadsperiode[0])));
    }

    public void lagreUttak(PerioderFraSøknad perioderFraSøknad, Long behandlingId) {
        uttakPerioderGrunnlagRepository.lagre(behandlingId, perioderFraSøknad);
    }

    public void oppdaterFagsakperiode(Optional<Periode> maksSøknadsperiode, Long fagsakId) {
        maksSøknadsperiode.ifPresent(periode -> fagsakRepository.utvidPeriode(fagsakId, periode.getFraOgMed(), periode.getTilOgMed()));
    }
}
