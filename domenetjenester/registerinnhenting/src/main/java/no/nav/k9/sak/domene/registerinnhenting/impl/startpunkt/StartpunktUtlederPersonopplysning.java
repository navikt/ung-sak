package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(PersonInformasjonEntitet.class)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
class StartpunktUtlederPersonopplysning implements EndringStartpunktUtleder {

    private final String source = this.getClass().getSimpleName();
    private PersonopplysningRepository personopplysningRepository;

    StartpunktUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    StartpunktUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object oppdatertGrunnlagId, Object forrigeGrunnlagId) {
        PersonopplysningGrunnlagEntitet oppdatertGrunnlag = personopplysningRepository.hentPersonopplysningerPåId((Long) oppdatertGrunnlagId);
        PersonopplysningGrunnlagEntitet forrigeGrunnlag = personopplysningRepository.hentPersonopplysningerPåId((Long) forrigeGrunnlagId);
        return utled(ref, oppdatertGrunnlag, forrigeGrunnlag);
    }

    private StartpunktType utled(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet oppdatertGrunnlag, PersonopplysningGrunnlagEntitet forrigeGrunnlag) {

        return hentAlleStartpunktForPersonopplysninger(ref, oppdatertGrunnlag, forrigeGrunnlag).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    // Finn endringer per aggregat under grunnlaget og map dem mot startpunkt. Dekker bruker og TPS-relaterte personer (barn, ekte). Bør spisses der det er behov.
    private List<StartpunktType> hentAlleStartpunktForPersonopplysninger(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet oppdatertGrunnlag, PersonopplysningGrunnlagEntitet forrigeGrunnlag) {
        var aktørId = ref.getAktørId();

        PersonopplysningGrunnlagDiff poDiff = new PersonopplysningGrunnlagDiff(aktørId, oppdatertGrunnlag, forrigeGrunnlag);
        boolean forelderDødEndret = poDiff.erForeldreDødsdatoEndret();
        boolean personstatusEndret = poDiff.erPersonstatusEndretForSøkerFør(null);
        boolean personstatusUnntattDødEndret = personstatusUnntattDødEndret(personstatusEndret, forelderDødEndret);

        Set<StartpunktType> startpunkter = new LinkedHashSet<>();
        if (forelderDødEndret) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UTTAKSVILKÅR, "foreldres død", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
        if (poDiff.erSivilstandEndretForBruker()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UTTAKSVILKÅR, "sivilstand", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
        if (poDiff.erBarnDødsdatoEndret()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.BEREGNING, "barnets dødsdato", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }

        final LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        if (personstatusUnntattDødEndret) {
            leggTilBasertPåSTP(oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag), startpunkter, poDiff.erPersonstatusEndretForSøkerFør(skjæringstidspunkt), "personstatus");
        }
        if (!Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN).contains(ref.getFagsakYtelseType()) && poDiff.erAdresserEndretFør(null)) {
            leggTilBasertPåSTP(oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag), startpunkter, poDiff.erSøkersAdresseEndretFør(skjæringstidspunkt), "adresse");
        }

        if (poDiff.erRegionEndretForBruker()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "region", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
            startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
        }
        if (poDiff.erRelasjonerEndret()) {
            leggTilForRelasjoner(oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag), poDiff, startpunkter);
        }
        if (startpunkter.isEmpty()) {
            // Endringen som trigget utledning av startpunkt skal ikke styre startpunkt
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UDEFINERT, "personopplysning - andre endringer", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
            startpunkter.add(StartpunktType.UDEFINERT);
        }
        return List.copyOf(startpunkter);
    }

    private Long håndtereNull(PersonopplysningGrunnlagEntitet forrigeGrunnlag) {
        return forrigeGrunnlag != null ? forrigeGrunnlag.getId() : null;
    }

    private void leggTilBasertPåSTP(Long g1Id, Long g2Id, Set<StartpunktType> startpunkter, boolean endretFørStp, String loggMelding) {
        if (endretFørStp) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, loggMelding, g1Id, g2Id);
            startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
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
