package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import java.time.LocalDate;
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

@ApplicationScoped
@GrunnlagRef("PersonInformasjon")
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("OMP")
@FagsakYtelseTypeRef("FRISINN")
class StartpunktUtlederPersonopplysning implements EndringStartpunktUtleder {

    private PersonopplysningRepository personopplysningRepository;

    private final String source = this.getClass().getSimpleName();

    StartpunktUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    StartpunktUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        PersonopplysningGrunnlagEntitet grunnlag1 = personopplysningRepository.hentPersonopplysningerPåId((Long)grunnlagId1);
        PersonopplysningGrunnlagEntitet grunnlag2 = personopplysningRepository.hentPersonopplysningerPåId((Long)grunnlagId2);
        return utled(ref, grunnlag1, grunnlag2);
    }

    private StartpunktType utled(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {

        return hentAlleStartpunktForPersonopplysninger(ref, grunnlag1, grunnlag2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    // Finn endringer per aggregat under grunnlaget og map dem mot startpunkt. Dekker bruker og TPS-relaterte personer (barn, ekte). Bør spisses der det er behov.
    private List<StartpunktType> hentAlleStartpunktForPersonopplysninger(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {
        var aktørId = ref.getAktørId();

        PersonopplysningGrunnlagDiff poDiff = new PersonopplysningGrunnlagDiff(aktørId, grunnlag1, grunnlag2);
        boolean forelderDødEndret = poDiff.erForeldreDødsdatoEndret();
        boolean personstatusEndret = poDiff.erPersonstatusEndretForSøkerFør(null);
        boolean personstatusUnntattDødEndret = personstatusUnntattDødEndret(personstatusEndret, forelderDødEndret);

        Set<StartpunktType> startpunkter = new LinkedHashSet<>();
        if (forelderDødEndret) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UTTAKSVILKÅR, "foreldres død", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
        if (poDiff.erSivilstandEndretForBruker()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UTTAKSVILKÅR, "sivilstand", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
        if (poDiff.erBarnDødsdatoEndret()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.BEREGNING, "barnets dødsdato", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.BEREGNING);
        }

        final LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        if (personstatusUnntattDødEndret) {
            leggTilBasertPåSTP(grunnlag1.getId(), grunnlag2.getId(), startpunkter, poDiff.erPersonstatusEndretForSøkerFør(skjæringstidspunkt), "personstatus");
        }
        if (poDiff.erAdresserEndretFør(null)) {
            leggTilBasertPåSTP(grunnlag1.getId(), grunnlag2.getId(), startpunkter, poDiff.erSøkersAdresseEndretFør(skjæringstidspunkt), "adresse");
        }

        if (poDiff.erRegionEndretForBruker()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "region", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
        }
        if(poDiff.erRelasjonerEndret()) {
            leggTilForRelasjoner(grunnlag1.getId(), grunnlag2.getId(), poDiff, startpunkter);
        }
        if (startpunkter.isEmpty()) {
            // Endringen som trigget utledning av startpunkt skal ikke styre startpunkt
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UDEFINERT, "personopplysning - andre endringer", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.UDEFINERT);
        }
        return List.copyOf(startpunkter);
    }

    private void leggTilBasertPåSTP(Long g1Id, Long g2Id, Set<StartpunktType> startpunkter, boolean endretFørStp, String loggMelding) {
        if (endretFørStp) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, loggMelding, g1Id, g2Id);
            startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
        } else {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UTTAKSVILKÅR, loggMelding, g1Id, g2Id);
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
    }

    private void leggTilForRelasjoner(Long g1Id, Long g2Id, PersonopplysningGrunnlagDiff poDiff, Set<StartpunktType> startpunkter) {
        if (poDiff.erRelasjonerEndretSøkerAntallBarn()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UDEFINERT, "personopplysning - relasjon på grunn av fødsel", g1Id, g2Id);
            startpunkter.add(StartpunktType.UDEFINERT);
        }

        var relasjonStartpunkt = StartpunktType.KONTROLLER_FAKTA;
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
