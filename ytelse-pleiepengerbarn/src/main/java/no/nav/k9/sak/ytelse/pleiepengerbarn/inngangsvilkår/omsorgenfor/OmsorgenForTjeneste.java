package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.BostedsAdresse;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.OmsorgenForVilkår;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.Relasjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell.RelasjonsRolle;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlagRepository;

@ApplicationScoped
public class OmsorgenForTjeneste {

    private VilkårUtfallOversetter utfallOversetter;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    OmsorgenForTjeneste() {
        // CDI
    }

    @Inject
    OmsorgenForTjeneste(OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository, BehandlingRepository behandlingRepository, BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.utfallOversetter = new VilkårUtfallOversetter();
        this.omsorgenForGrunnlagRepository = omsorgenForGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public List<VilkårData> vurderPerioder(BehandlingskontrollKontekst kontekst, Set<DatoIntervallEntitet> perioderTilVurdering) {
        var startDato = perioderTilVurdering.stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElse(LocalDate.now());
        var sluttDato = perioderTilVurdering.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElse(LocalDate.now());

        final var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);
        
        // TODO Omsorg: Håndtere nye vurderinger på utsiden av perioderTilVurdering?
        final OmsorgenForVilkårGrunnlag systemgrunnlag = oversettSystemdataTilRegelModellOmsorgen(kontekst.getBehandlingId(), kontekst.getAktørId(), periodeTilVurdering);
        final var vurdertOmsorgenForTidslinje = oversettTilRegelModellOmsorgenForVurderinger(kontekst);
        final var samletOmsorgenForTidslinje = slåSammenGrunnlagFraSystemOgVurdering(periodeTilVurdering, systemgrunnlag, vurdertOmsorgenForTidslinje);
        
        final List<VilkårData> resultat = new ArrayList<>();
        for (LocalDateSegment<OmsorgenForVilkårGrunnlag> s : samletOmsorgenForTidslinje.toSegments()) {
            final var evaluation = new OmsorgenForVilkår().evaluer(s.getValue());
            final var vilkårData = utfallOversetter.oversett(VilkårType.OMSORGEN_FOR, evaluation, s.getValue(), DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()));
            resultat.add(vilkårData);
        }

        return resultat;
    }

    private LocalDateTimeline<OmsorgenForVilkårGrunnlag> slåSammenGrunnlagFraSystemOgVurdering(final DatoIntervallEntitet periodeTilVurdering,
            final OmsorgenForVilkårGrunnlag systemgrunnlag,
            final LocalDateTimeline<OmsorgenForVilkårGrunnlag> vurdertOmsorgenForTidslinje) {
        return vurdertOmsorgenForTidslinje.combine(new LocalDateSegment<OmsorgenForVilkårGrunnlag>(periodeTilVurdering.toLocalDateInterval(), systemgrunnlag), new LocalDateSegmentCombinator<OmsorgenForVilkårGrunnlag, OmsorgenForVilkårGrunnlag, OmsorgenForVilkårGrunnlag>() {
            @Override
            public LocalDateSegment<OmsorgenForVilkårGrunnlag> combine(LocalDateInterval datoInterval, LocalDateSegment<OmsorgenForVilkårGrunnlag> datoSegment, LocalDateSegment<OmsorgenForVilkårGrunnlag> datoSegment2) {
                if (datoSegment == null) {
                    return new LocalDateSegment<OmsorgenForVilkårGrunnlag>(datoInterval, datoSegment2.getValue());
                }
                
                final OmsorgenForVilkårGrunnlag sg = datoSegment2.getValue();
                final OmsorgenForVilkårGrunnlag sammensatt = new OmsorgenForVilkårGrunnlag(sg.getRelasjonMellomSøkerOgPleietrengende(), sg.getSøkersAdresser(), sg.getPleietrengendeAdresser(), datoSegment.getValue().getErOmsorgsPerson());
                return new LocalDateSegment<OmsorgenForVilkårGrunnlag>(datoInterval, sammensatt);
            }
        }, JoinStyle.RIGHT_JOIN);
    }

    private LocalDateTimeline<OmsorgenForVilkårGrunnlag> oversettTilRegelModellOmsorgenForVurderinger(BehandlingskontrollKontekst kontekst) {
        final Optional<OmsorgenForGrunnlag> omsorgenForGrunnlag = omsorgenForGrunnlagRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        final var vurdertOmsorgenForTidslinje = new LocalDateTimeline<OmsorgenForVilkårGrunnlag>(
                    omsorgenForGrunnlag.map(og -> og.getOmsorgenFor().getPerioder()).orElse(List.of())
                    .stream()
                    .map(ofp -> new LocalDateSegment<OmsorgenForVilkårGrunnlag>(ofp.getPeriode().getFomDato(), ofp.getPeriode().getTomDato(), new OmsorgenForVilkårGrunnlag(null, null, null, mapToErOmsorgsperson(ofp.getResultat()))))
                    .collect(Collectors.toList())
                );
        return vurdertOmsorgenForTidslinje;
    }

    private Boolean mapToErOmsorgsperson(Resultat resultat) {
        switch (resultat) {
        case OPPFYLT: return true;
        case IKKE_OPPFYLT: return false;
        case IKKE_VURDERT: return null;
        default: throw new IllegalStateException("Uviklerfeil: Ukjen resultat: " + resultat);
        }
    }
    
    public OmsorgenForVilkårGrunnlag oversettSystemdataTilRegelModellOmsorgen(Long behandlingId, AktørId aktørId, DatoIntervallEntitet periodeTilVurdering) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periodeTilVurdering).orElseThrow();
        final var pleietrengende = behandlingRepository.hentBehandling(behandlingId).getFagsak().getPleietrengendeAktørId();
        
        // Lar denne stå her inntil videre selv om vi ikke bruker den:
        final var søkerBostedsadresser = personopplysningerAggregat.getAdresserFor(aktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        final var pleietrengendeBostedsadresser = personopplysningerAggregat.getAdresserFor(pleietrengende)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        
        // TODO OMSORG: Map inn verdi fremfor null:
        /*
        return new OmsorgenForVilkårGrunnlag(mapReleasjonMellomPleietrengendeOgSøker(personopplysningerAggregat, pleietrengende),
            mapAdresser(søkerBostedsadresser), mapAdresser(pleietrengendeBostedsadresser), omsorgenForGrunnlag.map(OmsorgenForGrunnlag::getOmsorgenFor).map(OmsorgenFor::getHarOmsorgFor).orElse(null));
            */
        
        return new OmsorgenForVilkårGrunnlag(mapReleasjonMellomPleietrengendeOgSøker(personopplysningerAggregat, pleietrengende),
                mapAdresser(søkerBostedsadresser), mapAdresser(pleietrengendeBostedsadresser), null);
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
