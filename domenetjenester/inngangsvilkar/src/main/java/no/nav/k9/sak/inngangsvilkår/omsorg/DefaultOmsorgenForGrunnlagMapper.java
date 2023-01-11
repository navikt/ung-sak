package no.nav.k9.sak.inngangsvilkår.omsorg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.BostedsAdresse;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForGrunnlagMapper;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.Relasjon;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.RelasjonsRolle;
import no.nav.k9.sak.typer.AktørId;

@FagsakYtelseTypeRef
@ApplicationScoped
public class DefaultOmsorgenForGrunnlagMapper implements OmsorgenForGrunnlagMapper {

    private PersonopplysningTjeneste personopplysningTjeneste;

    DefaultOmsorgenForGrunnlagMapper() {
        // CDI
    }

    @Inject
    public DefaultOmsorgenForGrunnlagMapper(PersonopplysningTjeneste personopplysningTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @Override
    public Map<DatoIntervallEntitet, OmsorgenForVilkårGrunnlag> map(BehandlingReferanse referanse, NavigableSet<DatoIntervallEntitet> perioder) {
        Map<DatoIntervallEntitet, OmsorgenForVilkårGrunnlag> result = new HashMap<>();

        for (DatoIntervallEntitet periode : perioder) {
            result.put(periode, mapGrunnlagForPeriode(referanse, periode));
        }

        return result;
    }

    private OmsorgenForVilkårGrunnlag mapGrunnlagForPeriode(BehandlingReferanse referanse, DatoIntervallEntitet periode) {
        var behandlingId = referanse.getBehandlingId();
        var aktørId = referanse.getAktørId();
        var pleietrengende = referanse.getPleietrengendeAktørId();
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periode).orElseThrow();

        // Lar denne stå her inntil videre selv om vi ikke bruker den:
        final var søkerBostedsadresser = personopplysningerAggregat.getAdresserFor(aktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        final var pleietrengendeBostedsadresser = personopplysningerAggregat.getAdresserFor(pleietrengende)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());

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
