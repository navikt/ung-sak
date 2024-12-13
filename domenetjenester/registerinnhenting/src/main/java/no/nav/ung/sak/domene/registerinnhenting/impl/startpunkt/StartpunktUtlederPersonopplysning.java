package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.domene.person.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
@GrunnlagRef(PersonInformasjonEntitet.class)
@FagsakYtelseTypeRef
class StartpunktUtlederPersonopplysning implements EndringStartpunktUtleder {

    private final String source = this.getClass().getSimpleName();
    private PersonopplysningRepository personopplysningRepository;
    private FagsakRepository fagsakRepository;

    StartpunktUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    StartpunktUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository,
                                      FagsakRepository fagsakRepository) {
        this.personopplysningRepository = personopplysningRepository;
        this.fagsakRepository = fagsakRepository;
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

        if (Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN, FagsakYtelseType.OLP).contains(ref.getFagsakYtelseType())) {
            Fagsak fagsak = fagsakRepository.finnEksaktFagsak(ref.getFagsakId());
            AktørId pleietrengendeAktørId = fagsak.getPleietrengendeAktørId();
            if (poDiff.erDødsdatoEndret(pleietrengendeAktørId)) {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.INNGANGSVILKÅR_MEDISINSK, "pletrengendes dødsdato", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
                startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDISINSK);
            }
        } else if (Set.of(FagsakYtelseType.OMP).contains(ref.getFagsakYtelseType())) {
            if (poDiff.erBarnDødsdatoEndret()) {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.BEREGNING, "barnets dødsdato", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
                startpunkter.add(StartpunktType.UTTAKSVILKÅR);
            }
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
