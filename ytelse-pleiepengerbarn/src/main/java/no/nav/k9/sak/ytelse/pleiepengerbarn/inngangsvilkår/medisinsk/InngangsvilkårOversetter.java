package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.medisinsk.LegeerklæringKilde;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.DiagnoseKilde;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.InnleggelsesPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskvilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PeriodeMedKontinuerligTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PeriodeMedUtvidetBehov;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.BostedsAdresse;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.OmsorgenForGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.Relasjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.RelasjonsRolle;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.KontinuerligTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.Legeerklæring;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk.OmsorgenFor;

@ApplicationScoped
public class InngangsvilkårOversetter {

    private BehandlingRepository behandlingRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;

    InngangsvilkårOversetter() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOversetter(MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                    BehandlingRepository behandlingRepository,
                                    BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public MedisinskvilkårGrunnlag oversettTilRegelModellMedisinsk(Long behandlingId, DatoIntervallEntitet periode) {
        final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId);

        final var vilkårsGrunnlag = new MedisinskvilkårGrunnlag(periode.getFomDato(), periode.getTomDato());
        if (medisinskGrunnlag.isPresent()) {
            var grunnlag = medisinskGrunnlag.get();
            var legeerkleringer = grunnlag.getLegeerklæringer()
                .getLegeerklæringer()
                .stream()
                .map(le -> new LocalDateSegment<>(le.getDatert(), Tid.TIDENES_ENDE, le))
                .collect(Collectors.toList());
            final var timeline = new LocalDateTimeline<>(legeerkleringer);
            var relevanteLegeerklæringer = timeline.intersection(new LocalDateInterval(periode.getFomDato(), periode.getTomDato()))
                .toSegments()
                .stream()
                .map(LocalDateSegment::getValue)
                .collect(Collectors.toList());

            final var relevantKontinuerligTilsyn = grunnlag.getKontinuerligTilsyn()
                .getPerioder()
                .stream()
                .filter(it -> it.getPeriode().overlapper(periode))
                .collect(Collectors.toList());

            vilkårsGrunnlag.medDiagnoseKilde(utledKildeFraLegeerklæringer(relevanteLegeerklæringer))
                .medDiagnoseKode(utledDiagnose(relevanteLegeerklæringer))
                .medInnleggelsesPerioder(mapInnleggelsesPerioder(relevanteLegeerklæringer))
                .medKontinuerligTilsyn(mapKontinuerligTilsyn(relevantKontinuerligTilsyn))
                .medUtvidetBehov(mapUtvidetTilsyn(relevantKontinuerligTilsyn));
        }
        return vilkårsGrunnlag;
    }

    private List<PeriodeMedUtvidetBehov> mapUtvidetTilsyn(List<KontinuerligTilsynPeriode> relevantKontinuerligTilsyn) {
        return relevantKontinuerligTilsyn.stream()
            .filter(it -> it.getGrad() == 200)
            .map(it -> new PeriodeMedUtvidetBehov(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .collect(Collectors.toList());
    }

    private List<PeriodeMedKontinuerligTilsyn> mapKontinuerligTilsyn(List<KontinuerligTilsynPeriode> relevantKontinuerligTilsyn) {
        return relevantKontinuerligTilsyn.stream()
            .filter(it -> it.getGrad() == 100)
            .filter(KontinuerligTilsynPeriode::getÅrsaksammenheng)
            .map(it -> new PeriodeMedKontinuerligTilsyn(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .collect(Collectors.toList());
    }

    private List<InnleggelsesPeriode> mapInnleggelsesPerioder(List<Legeerklæring> relevanteLegeerklæringer) {
        return relevanteLegeerklæringer.stream()
            .map(Legeerklæring::getInnleggelsesPerioder)
            .flatMap(Collection::stream)
            .map(it -> new InnleggelsesPeriode(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()))
            .collect(Collectors.toList());
    }

    private String utledDiagnose(List<Legeerklæring> legeerklæringer) {
        return legeerklæringer.stream()
            .filter(it -> it.getDiagnose() != null)
            .filter(it -> !it.getDiagnose().isEmpty())
            .filter(it -> !it.getDiagnose().isBlank())
            .min(Comparator.comparing(Legeerklæring::getDatert, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(Legeerklæring::getDiagnose)
            .orElse(null);
    }

    private DiagnoseKilde utledKildeFraLegeerklæringer(List<Legeerklæring> legeerklæringer) {
        final var kilder = legeerklæringer
            .stream()
            .map(Legeerklæring::getKilde)
            .collect(Collectors.toSet());
        if (kilder.contains(LegeerklæringKilde.SYKEHUSLEGE)) {
            return DiagnoseKilde.SYKHUSLEGE;
        }
        if (kilder.contains(LegeerklæringKilde.SPESIALISTHELSETJENESTE)) {
            return DiagnoseKilde.SPESIALISTHELSETJENESTEN;
        }
        if (kilder.contains(LegeerklæringKilde.FASTLEGE)) {
            return DiagnoseKilde.FASTLEGE;
        }
        return DiagnoseKilde.ANNET;
    }

    public OmsorgenForGrunnlag oversettTilRegelModellOmsorgen(Long behandlingId, AktørId aktørId, DatoIntervallEntitet periodeTilVurdering) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periodeTilVurdering).orElseThrow();
        final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId);
        final var pleietrengende = behandlingRepository.hentBehandling(behandlingId).getFagsak().getPleietrengendeAktørId();
        final var søkerBostedsadresser = personopplysningerAggregat.getAdresserFor(aktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        final var pleietrengendeBostedsadresser = personopplysningerAggregat.getAdresserFor(pleietrengende)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        return new OmsorgenForGrunnlag(mapReleasjonMellomPleietrengendeOgSøker(personopplysningerAggregat, pleietrengende),
            mapAdresser(søkerBostedsadresser), mapAdresser(pleietrengendeBostedsadresser), medisinskGrunnlag.map(MedisinskGrunnlag::getOmsorgenFor).map(OmsorgenFor::getHarOmsorgFor).orElse(null));
    }

    private List<BostedsAdresse> mapAdresser(List<PersonAdresseEntitet> pleietrengendeBostedsadresser) {
        return pleietrengendeBostedsadresser.stream()
            .map(it -> new BostedsAdresse(it.getAktørId().getId(), it.getAdresselinje1(), it.getAdresselinje2(), it.getAdresselinje3(), it.getPostnummer(), it.getLand()))
            .collect(Collectors.toList());
    }

    private Relasjon mapReleasjonMellomPleietrengendeOgSøker(PersonopplysningerAggregat aggregat, AktørId pleietrengende) {
        final var relasjoner = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengende)).collect(Collectors.toSet());
        if (relasjoner.size() > 1) {
            throw new IllegalStateException("Fant flere relasjoner til barnet. Vet ikke hvilken som skal prioriteres");
        } else if (relasjoner.size() == 1) {
            final var relasjonen = relasjoner.iterator().next();
            return new Relasjon(relasjonen.getAktørId().getId(), relasjonen.getTilAktørId().getId(), RelasjonsRolle.find(relasjonen.getRelasjonsrolle().getKode()), relasjonen.getHarSammeBosted());
        } else {
            return null;
        }
    }
}
