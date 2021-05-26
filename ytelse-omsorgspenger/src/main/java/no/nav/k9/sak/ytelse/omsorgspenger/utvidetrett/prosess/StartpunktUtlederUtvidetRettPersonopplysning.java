package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt.FellesStartpunktUtlederLogger;

@ApplicationScoped
@GrunnlagRef("PersonInformasjon")
@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@FagsakYtelseTypeRef("OMP_AO")
class StartpunktUtlederUtvidetRettPersonopplysning implements EndringStartpunktUtleder {

    private static final StartpunktType DEFAULT_STARTPUNKT = StartpunktType.INIT_PERIODER;
    private final String source = this.getClass().getSimpleName();

    private PersonopplysningRepository personopplysningRepository;

    StartpunktUtlederUtvidetRettPersonopplysning() {
        // For CDI
    }

    @Inject
    StartpunktUtlederUtvidetRettPersonopplysning(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        PersonopplysningGrunnlagEntitet grunnlag1 = personopplysningRepository.hentPersonopplysningerPåId((Long) grunnlagId1);
        PersonopplysningGrunnlagEntitet grunnlag2 = personopplysningRepository.hentPersonopplysningerPåId((Long) grunnlagId2);
        return utled(ref, grunnlag1, grunnlag2);
    }

    private StartpunktType utled(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {

        return hentAlleStartpunktForPersonopplysninger(ref, grunnlag1, grunnlag2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    // Finn endringer per aggregat under grunnlaget og map dem mot startpunkt. Dekker bruker og TPS-relaterte personer (barn, ekte). Bør spisses
    // der det er behov.
    private List<StartpunktType> hentAlleStartpunktForPersonopplysninger(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {
        var aktørId = ref.getAktørId();

        PersonopplysningGrunnlagDiff poDiff = new PersonopplysningGrunnlagDiff(aktørId, grunnlag1, grunnlag2);
        boolean forelderDødEndret = poDiff.erForeldreDødsdatoEndret();
        boolean personstatusEndret = poDiff.erPersonstatusEndretForSøkerFør(null);
        boolean personstatusUnntattDødEndret = personstatusUnntattDødEndret(personstatusEndret, forelderDødEndret);

        Set<StartpunktType> startpunkter = new LinkedHashSet<>();
        if (forelderDødEndret) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UTTAKSVILKÅR, "foreldres død", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(DEFAULT_STARTPUNKT);
        }
        if (poDiff.erSivilstandEndretForBruker()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UTTAKSVILKÅR, "sivilstand", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(DEFAULT_STARTPUNKT);
        }
        if (poDiff.erBarnDødsdatoEndret()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.BEREGNING, "barnets dødsdato", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(DEFAULT_STARTPUNKT);
        }
        if (personstatusUnntattDødEndret) {
            leggTilStartpunkt(grunnlag1.getId(), grunnlag2.getId(), startpunkter, "personstatus");
        }
        if (poDiff.erAdresserEndretFør(null)) {
            leggTilStartpunkt(grunnlag1.getId(), grunnlag2.getId(), startpunkter, "adresse");
        }
        if (poDiff.erRegionEndretForBruker()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "region", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(DEFAULT_STARTPUNKT);
        }
        if (poDiff.erRelasjonerEndret()) {
            leggTilForRelasjoner(grunnlag1.getId(), grunnlag2.getId(), poDiff, startpunkter);
        }

        if (startpunkter.isEmpty()) {
            // Endringen som trigget utledning av startpunkt skal ikke styre startpunkt
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UDEFINERT, "personopplysning - andre endringer", grunnlag1.getId(),
                grunnlag2.getId());
            startpunkter.add(StartpunktType.UDEFINERT);
        }

        return List.copyOf(startpunkter);
    }

    private void leggTilStartpunkt(Long g1Id, Long g2Id, Set<StartpunktType> startpunkter, String loggMelding) {
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, DEFAULT_STARTPUNKT, loggMelding, g1Id, g2Id);
        startpunkter.add(DEFAULT_STARTPUNKT);
    }

    private void leggTilForRelasjoner(Long g1Id, Long g2Id, PersonopplysningGrunnlagDiff poDiff, Set<StartpunktType> startpunkter) {
        if (poDiff.erRelasjonerEndretSøkerAntallBarn()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UDEFINERT, "personopplysning - relasjon på grunn av fødsel", g1Id, g2Id);
            startpunkter.add(StartpunktType.UDEFINERT);
        }

        var relasjonStartpunkt = DEFAULT_STARTPUNKT;
        if (poDiff.erRelasjonerEndretForSøkerUtenomNyeBarn()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, relasjonStartpunkt, "personopplysning - brukers relasjoner annet enn fødsel", g1Id, g2Id);
            startpunkter.add(relasjonStartpunkt);
        }
        if (poDiff.erRelasjonerEndretForEksisterendeBarn()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, relasjonStartpunkt, "personopplysning - barns relasjoner annet enn fødsel", g1Id, g2Id);
            startpunkter.add(relasjonStartpunkt);
        }
    }

    private boolean personstatusUnntattDødEndret(boolean personstatusEndret, boolean søkerErDødEndret) {
        return personstatusEndret && !søkerErDødEndret;
    }
}
