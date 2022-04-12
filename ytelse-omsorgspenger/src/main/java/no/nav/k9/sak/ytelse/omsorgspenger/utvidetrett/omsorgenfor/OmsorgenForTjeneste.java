package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell.BostedsAdresse;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell.OmsorgenForVilkår;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell.Relasjon;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.omsorgenfor.regelmodell.RelasjonsRolle;

@Dependent
public class OmsorgenForTjeneste {

    private VilkårUtfallOversetter utfallOversetter;
    private BehandlingRepository behandlingRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    OmsorgenForTjeneste(BehandlingRepository behandlingRepository, BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.utfallOversetter = new VilkårUtfallOversetter();
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public List<VilkårData> vurderPerioder(LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje) {
        final List<VilkårData> resultat = new ArrayList<>();
        for (LocalDateSegment<OmsorgenForVilkårGrunnlag> s : samletOmsorgenForTidslinje.toSegments()) {
            final var evaluation = new OmsorgenForVilkår().evaluer(s.getValue());
            final var vilkårData = utfallOversetter.oversett(VilkårType.OMSORGEN_FOR, evaluation, s.getValue(), DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()));
            resultat.add(vilkårData);
        }
        return resultat;
    }

    public LocalDateTimeline<OmsorgenForVilkårGrunnlag> oversettSystemdataTilRegelModellGrunnlag(Long behandlingId, Collection<VilkårPeriode> vilkårsperioder) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Fagsak fagsak = behandling.getFagsak();
        var søkerAktørId = fagsak.getAktørId();
        var barnAktørId = fagsak.getPleietrengendeAktørId();

        var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, søkerAktørId, omsluttendePeriode(vilkårsperioder)).orElseThrow();
        var søkerBostedsadresser = personopplysningerAggregat.getAdresserFor(søkerAktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .toList();
        var barnBostedsadresser = personopplysningerAggregat.getAdresserFor(barnAktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .toList();
        var deltBostedAdresser = personopplysningerAggregat.getAdresserFor(barnAktørId)
            .stream()
            .filter(it -> AdresseType.DELT_BOSTEDSADRESSE.equals(it.getAdresseType()))
            .toList();

        return new LocalDateTimeline<>(vilkårsperioder.stream()
            .map(vilkårsperiode ->
                new LocalDateSegment<>(vilkårsperiode.getFom(), vilkårsperiode.getTom(), new OmsorgenForVilkårGrunnlag(
                    mapRelasjonMellomSøkerOgBarn(personopplysningerAggregat, barnAktørId),
                    mapAdresser(søkerBostedsadresser),
                    mapAdresser(barnBostedsadresser),
                    mapDeltBostedAdresser(deltBostedAdresser, vilkårsperiode))
                )
            )
            .toList()
        );
    }

    private Map<Periode, List<BostedsAdresse>> mapDeltBostedAdresser(List<PersonAdresseEntitet> perioderDeltBosted, VilkårPeriode vilkårsperiode) {
        return perioderDeltBosted.stream()
            .filter(p -> vilkårsperiode.getPeriode().overlapper(p.getPeriode()))
            .collect(Collectors.groupingBy(p -> new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato())))
            .entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> mapAdresser(e.getValue())));
    }

    private DatoIntervallEntitet omsluttendePeriode(Collection<VilkårPeriode> perioder) {
        var startDato = perioder.stream().map(VilkårPeriode::getFom).min(LocalDate::compareTo).orElse(LocalDate.now());
        var sluttDato = perioder.stream().map(VilkårPeriode::getTom).max(LocalDate::compareTo).orElse(LocalDate.now());
        return DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);
    }

    private List<BostedsAdresse> mapAdresser(List<PersonAdresseEntitet> bostedsadresser) {
        return bostedsadresser.stream()
            .map(it -> new BostedsAdresse(it.getPeriode().tilPeriode(), it.getAktørId().getId(), it.getAdresselinje1(), it.getAdresselinje2(), it.getAdresselinje3(), it.getPostnummer(), it.getLand()))
            .toList();
    }

    private Relasjon mapRelasjonMellomSøkerOgBarn(PersonopplysningerAggregat aggregat, AktørId barnAktørId) {
        final var relasjoner = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(barnAktørId)).collect(Collectors.toSet());
        if (relasjoner.size() > 1) {
            throw new IllegalStateException("Fant flere relasjoner til barnet. Vet ikke hvilken som skal prioriteres");
        } else if (relasjoner.size() == 1) {
            final var relasjonen = relasjoner.iterator().next();
            return new Relasjon(relasjonen.getAktørId().getId(), relasjonen.getTilAktørId().getId(), RelasjonsRolle.find(relasjonen.getRelasjonsrolle().getKode()));
        } else {
            return null;
        }
    }

}
