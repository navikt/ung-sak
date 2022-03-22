package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell.AleneOmOmsorgenVilkårGrunnlag;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell.BostedsAdresse;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell.OmsorgenForVilkår;

@Dependent
public class AleneOmOmsorgenTjeneste {

    private VilkårUtfallOversetter utfallOversetter;
    private BehandlingRepository behandlingRepository;
    private TpsTjeneste tpsTjeneste;

    @Inject
    AleneOmOmsorgenTjeneste(BehandlingRepository behandlingRepository, TpsTjeneste tpsTjeneste) {
        this.utfallOversetter = new VilkårUtfallOversetter();
        this.behandlingRepository = behandlingRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    public List<VilkårData> vurderPerioder(LocalDateTimeline<AleneOmOmsorgenVilkårGrunnlag> samletOmsorgenForTidslinje) {
        final List<VilkårData> resultat = new ArrayList<>();
        for (LocalDateSegment<AleneOmOmsorgenVilkårGrunnlag> s : samletOmsorgenForTidslinje.toSegments()) {
            final var evaluation = new OmsorgenForVilkår().evaluer(s.getValue());
            final var vilkårData = utfallOversetter.oversett(VilkårType.OMSORGEN_FOR, evaluation, s.getValue(), DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()));
            resultat.add(vilkårData);
        }
        return resultat;
    }

    public LocalDateTimeline<AleneOmOmsorgenVilkårGrunnlag> oversettSystemdataTilRegelModellGrunnlag(Long behandlingId, Collection<VilkårPeriode> vilkårsperioder) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Fagsak fagsak = behandling.getFagsak();
        var søkerAktørId = fagsak.getAktørId();
        var barnAktørId = fagsak.getPleietrengendeAktørId();

        AktørId annenForelderAktørId = finnAnnenForelder(søkerAktørId, barnAktørId);
        List<BostedsAdresse> søkerBostedsadresser = hentBostedAdresser(søkerAktørId);
        List<BostedsAdresse> annenForeldersBostedAdresser = hentBostedAdresser(annenForelderAktørId);

        return new LocalDateTimeline<>(vilkårsperioder.stream()
            .map(vilkårsperiode -> new LocalDateSegment<>(vilkårsperiode.getFom(), vilkårsperiode.getTom(), new AleneOmOmsorgenVilkårGrunnlag(annenForelderAktørId, søkerBostedsadresser, annenForeldersBostedAdresser)))
            .toList()
        );
    }

    private AktørId finnAnnenForelder(AktørId søkerAktørId, AktørId barnAktørId) {
        Personinfo personinfoBarnet = tpsTjeneste.hentBrukerForAktør(barnAktørId).orElseThrow();

        Set<RelasjonsRolleType> foreldreroller = Set.of(RelasjonsRolleType.FARA, RelasjonsRolleType.MORA, RelasjonsRolleType.MEDMOR);
        List<PersonIdent> foreldreFnr = personinfoBarnet.getFamilierelasjoner().stream()
            .filter(familierelasjon -> foreldreroller.contains(familierelasjon.getRelasjonsrolle()))
            .map(Familierelasjon::getPersonIdent)
            .distinct()
            .toList();

        List<AktørId> andreForeldre = foreldreFnr.stream()
            .map(personIdent -> tpsTjeneste.hentAktørForFnr(personIdent).orElseThrow())
            .filter(aktørId -> !Objects.equals(aktørId, søkerAktørId))
            .distinct()
            .toList();
        if (andreForeldre.size() > 1) {
            throw new IllegalArgumentException("Fant flere enn 1 andre foreldre for barnet");
        }
        return andreForeldre.isEmpty() ? null : andreForeldre.get(0);
    }

    private List<BostedsAdresse> hentBostedAdresser(AktørId annenForelder) {
        if (annenForelder != null) {
            Personinfo personInfoAnnenForelder = tpsTjeneste.hentBrukerForAktør(annenForelder).orElseThrow();
            return personInfoAnnenForelder.getAdresseInfoList().stream()
                .filter(adresseinfo -> adresseinfo.getGjeldendePostadresseType() == AdresseType.BOSTEDSADRESSE)
                .map(adresseinfo -> new BostedsAdresse(annenForelder.getAktørId(), adresseinfo.getAdresselinje1(), adresseinfo.getAdresselinje2(), adresseinfo.getAdresselinje3(), adresseinfo.getPostNr(), adresseinfo.getLand()))
                .toList();
        } else {
            return List.of();
        }
    }

}
