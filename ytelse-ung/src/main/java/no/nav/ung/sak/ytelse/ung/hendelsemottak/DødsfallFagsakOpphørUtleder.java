package no.nav.ung.sak.ytelse.ung.hendelsemottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.ung.sak.kontrakt.hendelser.DødsfallHendelse;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@HendelseTypeRef("PDL_DØDSFALL")
public class DødsfallFagsakOpphørUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(DødsfallFagsakOpphørUtleder.class);
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private PersonopplysningRepository personopplysningRepository;
    private PersoninfoAdapter personinfoAdapter;

    public DødsfallFagsakOpphørUtleder() {
        // For CDI
    }

    @Inject
    public DødsfallFagsakOpphørUtleder(FagsakRepository fagsakRepository,
                                       BehandlingRepository behandlingRepository,
                                       VilkårResultatRepository vilkårResultatRepository,
                                       PersonopplysningRepository personopplysningRepository,
                                       PersoninfoAdapter personinfoAdapter) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.personopplysningRepository = personopplysningRepository;
        this.personinfoAdapter = personinfoAdapter;
    }

    @Override
    public Map<Fagsak, BehandlingÅrsakType> finnFagsakerTilVurdering(Hendelse hendelse) {
        DødsfallHendelse dødsfallHendelse = (DødsfallHendelse) hendelse;

        List<AktørId> dødsfallAktører = dødsfallHendelse.getHendelseInfo().getAktørIder();
        LocalDate dødsdatoFraHendelse = dødsfallHendelse.getHendelsePeriode().getFom();
        String hendelseId = dødsfallHendelse.getHendelseInfo().getHendelseId();
        LocalDateTime hendelseOpprettetTidspunkt = dødsfallHendelse.getHendelseInfo().getOpprettet();

        var fagsaker = new HashMap<Fagsak, BehandlingÅrsakType>();

        dødsfallAktører.forEach(aktør -> {
            var fagsakerForBruker = fagsakRepository.hentForBruker(aktør);
            if (fagsakerForBruker.isEmpty()) {
                return;
            }
            LocalDate dødsdato = hentOppdatertDødsdato(aktør, dødsdatoFraHendelse, hendelseOpprettetTidspunkt, hendelseId);
            for (Fagsak fagsak : finnOverlappendeFagsaker(fagsakerForBruker, dødsdato, FagsakYtelseType.UNGDOMSYTELSE)) {
                if (erNyInformasjonIHendelsen(fagsak, aktør, dødsdato, hendelseId) && erAktørensDødRelevantForVedtaket(fagsak, dødsdato)) {
                    fagsaker.put(fagsak, BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);
                }
            }
        });

        return fagsaker;
    }

    private static Set<Fagsak> finnOverlappendeFagsaker(List<Fagsak> fagsakerForSøker, LocalDate dødsdato, FagsakYtelseType fagsakYtelseType) {
        return fagsakerForSøker.stream()
            .filter(fagsak -> fagsak.getYtelseType().equals(fagsakYtelseType))
            .filter(f -> f.getPeriode().overlapper(dødsdato, AbstractLocalDateInterval.TIDENES_ENDE)).collect(Collectors.toSet());
    }

    private boolean erAktørensDødRelevantForVedtaket(Fagsak fagsak, LocalDate dødsdato) {

        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        if (!behandlingOpt.get().erAvsluttet()) {
            return true;
        }

        var vilkårresultat = vilkårResultatRepository.hent(behandlingOpt.get().getId());
        var heleVilkårTidslinjen = vilkårresultat.getAlleIntervaller();
        var avslåttTidslinje = finnAvslåttTidslinje(vilkårresultat);
        var innvilgetTidslinje = heleVilkårTidslinjen.disjoint(avslåttTidslinje);
        var innvilgelseFomDødsdato = innvilgetTidslinje.intersection(new LocalDateInterval(dødsdato, fagsak.getPeriode().getTomDato()));
        var erRelevant = !innvilgelseFomDødsdato.isEmpty();
        if (!erRelevant) {
            logger.info("Mottok dødshendelse som ble vurdert som ikke relevant for fagsak " + fagsak.getId());
        }
        return erRelevant;
    }

    private static LocalDateTimeline<Boolean> finnAvslåttTidslinje(Vilkårene vilkårresultat) {
        var avslåtteSegmenter = vilkårresultat.getVilkårene().stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(v -> v.getUtfall().equals(Utfall.IKKE_OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .map(p -> new LocalDateSegment<>(p.toLocalDateInterval(), true))
            .toList();
        return new LocalDateTimeline<>(avslåtteSegmenter, StandardCombinators::alwaysTrueForMatch);
    }

    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * hindrer også revurdering hvis hendelsen kommer etter at behandlingen er oppdatert med ny data.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, AktørId aktør, LocalDate dødsdato, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        PersonopplysningGrunnlagEntitet personopplysninger = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        if (personopplysninger != null) {
            for (PersonopplysningEntitet personopplysning : personopplysninger.getGjeldendeVersjon().getPersonopplysninger()) {
                if (aktør.equals(personopplysning.getAktørId()) && Objects.equals(dødsdato, personopplysning.getDødsdato())) {
                    logger.info("Persondata på behandling {} for {} var allerede oppdatert med riktig dødsdato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Vi forventer å håndtere hendelser som er utdaterte, eksempelvis at topic leses på nytt fra start. Derfor gjør vi en sjekk
     * mot API-et fra PDL i tilfelle dødsdato skal være korrigert etter at hendelsen ble opprettet, for å unngå å feilaktig trigge revurdering.
     */
    private LocalDate hentOppdatertDødsdato(AktørId aktør, LocalDate dødsdatoFraHendelse, LocalDateTime hendelseOpprettetTidspunkt, String hendelseId) {
        LocalDate dødsdatoFraApi;

        try {
            Personinfo personinfo = personinfoAdapter.hentPersoninfo(aktør);
            dødsdatoFraApi = personinfo.getDødsdato();
        } catch (NullPointerException e) {
            if (Environment.current().isDev() && "Navbruker må ha navn".equals(e.getMessage())) {
                logger.warn("Ignorerte dødshendelse for bruker som ikke har navn, skjer kun i dev. Hendelseid {}", hendelseId);
                return null;
            }
            throw e;
        }

        if (dødsdatoFraApi == null) {
            logger.warn("Mottok dødshendelse {} opprettet {}, men API-et fra PDL har ikke registrert dødsfall på aktuell person. Går videre med ingen dødsdato.", hendelseId, hendelseOpprettetTidspunkt);
        } else if (!dødsdatoFraApi.equals(dødsdatoFraHendelse)) {
            logger.warn("Mottok dødshendelse {} opprettet {}, men API-et fra PDL har ikke registrert samme dato på aktuell person. Går videre med dato fra API-et.", hendelseId, hendelseOpprettetTidspunkt);
        }
        return dødsdatoFraApi;
    }

}
