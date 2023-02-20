package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.person.pdl.PersoninfoTjeneste;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell.AleneomsorgVilkår;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell.AleneomsorgVilkårGrunnlag;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell.BostedsAdresse;

@Dependent
public class AleneomsorgTjeneste {

    private VilkårUtfallOversetter utfallOversetter;
    private BehandlingRepository behandlingRepository;
    private TpsTjeneste tpsTjeneste;
    private PersoninfoTjeneste personinfoTjeneste;

    @Inject
    AleneomsorgTjeneste(BehandlingRepository behandlingRepository, TpsTjeneste tpsTjeneste, PersoninfoTjeneste personinfoTjeneste) {
        this.utfallOversetter = new VilkårUtfallOversetter();
        this.behandlingRepository = behandlingRepository;
        this.tpsTjeneste = tpsTjeneste;
        this.personinfoTjeneste = personinfoTjeneste;
    }

    public List<VilkårData> vurderPerioder(LocalDateTimeline<AleneomsorgVilkårGrunnlag> samletOmsorgenForTidslinje) {
        List<VilkårData> resultat = new ArrayList<>();
        for (LocalDateSegment<AleneomsorgVilkårGrunnlag> s : samletOmsorgenForTidslinje.toSegments()) {
            var evaluation = new AleneomsorgVilkår().evaluer(s.getValue());
            var vilkårData = utfallOversetter.oversett(VilkårType.OMSORGEN_FOR, evaluation, s.getValue(), DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()));
            resultat.add(vilkårData);
        }
        return resultat;
    }

    public LocalDateTimeline<AleneomsorgVilkårGrunnlag> oversettSystemdataTilRegelModellGrunnlag(Long behandlingId, Collection<VilkårPeriode> vilkårsperioder) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Fagsak fagsak = behandling.getFagsak();
        var søkerAktørId = fagsak.getAktørId();
        var barnAktørId = fagsak.getPleietrengendeAktørId();
        Periode periodeForDatainnhenting = omsluttendePeriode(vilkårsperioder);
        Map<AktørId, List<BostedsAdresse>> foreldreBostedAdresser = finnForeldresAdresser(barnAktørId, periodeForDatainnhenting);
        List<BostedsAdresse> søkerBostedAdresser = finnBostedAdresser(søkerAktørId, periodeForDatainnhenting);
        List<BostedsAdresse> barnetsDeltBostedAdresser = finnDeltBostedAdresser(barnAktørId, periodeForDatainnhenting);

        return new LocalDateTimeline<>(vilkårsperioder.stream()
            .map(vilkårsperiode -> new LocalDateSegment<>(vilkårsperiode.getFom(), vilkårsperiode.getTom(), new AleneomsorgVilkårGrunnlag(søkerAktørId, søkerBostedAdresser, foreldreBostedAdresser, barnetsDeltBostedAdresser)))
            .toList()
        );
    }

    private Periode omsluttendePeriode(Collection<VilkårPeriode> vilkårPerioder) {
        return new Periode(
            vilkårPerioder.stream().map(VilkårPeriode::getFom).min(Comparator.naturalOrder()).orElseThrow(),
            vilkårPerioder.stream().map(VilkårPeriode::getTom).max(Comparator.naturalOrder()).orElseThrow()
        );
    }

    private Map<AktørId, List<BostedsAdresse>> finnForeldresAdresser(AktørId barnAktørId, Periode periode) {
        Set<AktørId> foreldre = finnForeldre(barnAktørId);
        return foreldre.stream()
            .collect(Collectors.toMap(forelder -> forelder, forelder -> finnBostedAdresser(forelder, periode)));
    }

    private Set<AktørId> finnForeldre(AktørId barnAktørId) {
        Personinfo personinfoBarnet = tpsTjeneste.hentBrukerForAktør(barnAktørId).orElseThrow();

        Set<RelasjonsRolleType> foreldreroller = Set.of(RelasjonsRolleType.FARA, RelasjonsRolleType.MORA, RelasjonsRolleType.MEDMOR);
        List<PersonIdent> foreldreFnr = personinfoBarnet.getFamilierelasjoner().stream()
            .filter(familierelasjon -> foreldreroller.contains(familierelasjon.getRelasjonsrolle()))
            .map(Familierelasjon::getPersonIdent)
            .distinct()
            .toList();

        return foreldreFnr.stream()
            .map(personIdent -> tpsTjeneste.hentAktørForFnr(personIdent).orElseThrow())
            .collect(Collectors.toSet());
    }

    private List<BostedsAdresse> finnBostedAdresser(AktørId aktørId, Periode periode) {
        Personhistorikkinfo personhistorikkinfo = personinfoTjeneste.hentPersoninfoHistorikk(aktørId, periode);
        return personhistorikkinfo.getAdressehistorikk().stream()
            .filter(adressePeriode -> overlapper(periode, adressePeriode))
            .filter(adressePeriode -> adressePeriode.getAdresse().getAdresseType() == AdresseType.BOSTEDSADRESSE)
            .map(adressePeriode -> {
                AdressePeriode.Adresse adresse = adressePeriode.getAdresse();
                Periode p = tilPeriode(adressePeriode.getGyldighetsperiode());
                return new BostedsAdresse(p, adresse.getAdresselinje1(), adresse.getAdresselinje2(), adresse.getAdresselinje3(), adresse.getPostnummer(), adresse.getLand());
            })
            .toList();
    }

    private List<BostedsAdresse> finnDeltBostedAdresser(AktørId barnAktørId, Periode periodeForDatainnhenting) {
        Personinfo personinfoBarnet = tpsTjeneste.hentBrukerForAktør(barnAktørId).orElseThrow();
        return personinfoBarnet.getDeltBostedList()
            .stream()
            .filter(deltBosted -> periodeForDatainnhenting.overlaps(deltBosted.getPeriode()))
            .map(deltBosted -> new BostedsAdresse(deltBosted.getPeriode(), deltBosted.getAdresseinfo().getAdresselinje1(), deltBosted.getAdresseinfo().getAdresselinje2(), deltBosted.getAdresseinfo().getAdresselinje3(), deltBosted.getAdresseinfo().getPostNr(), deltBosted.getAdresseinfo().getLand()))
            .toList();
    }

    private boolean overlapper(Periode periode, AdressePeriode adresseinfo) {
        return periode.overlaps(tilPeriode(adresseinfo.getGyldighetsperiode()));
    }

    private Periode tilPeriode(Gyldighetsperiode gyldighetsperiode) {
        return new Periode(gyldighetsperiode.getFom(), gyldighetsperiode.getTom());
    }

}
