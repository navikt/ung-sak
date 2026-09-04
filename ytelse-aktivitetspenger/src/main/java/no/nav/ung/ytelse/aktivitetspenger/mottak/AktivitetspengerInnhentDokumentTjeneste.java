package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.repository.*;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.*;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.mottak.Behandlingsoppretter;
import no.nav.ung.sak.mottak.dokumentmottak.*;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerInnhentDokumentTjeneste implements InnhentDokumentTjeneste {

    private static final Logger log = LoggerFactory.getLogger(AktivitetspengerInnhentDokumentTjeneste.class);

    private final Instance<Dokumentmottaker> mottakere;
    private final Behandlingsoppretter behandlingsoppretter;
    private final BehandlingRepository behandlingRepository;
    private final BehandlingRevurderingRepository revurderingRepository;
    private final BehandlingLåsRepository behandlingLåsRepository;
    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private final ProsessTaskTjeneste prosessTaskTjeneste;
    private final FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private final ProsessTriggereRepository prosessTriggereRepository;
    private final VilkårResultatRepository vilkårResultatRepository;


    @Inject
    public AktivitetspengerInnhentDokumentTjeneste(@Any Instance<Dokumentmottaker> mottakere,
                                                   Behandlingsoppretter behandlingsoppretter,
                                                   BehandlingRepositoryProvider repositoryProvider, BehandlingRevurderingRepository revurderingRepository,
                                                   BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                                   ProsessTaskTjeneste prosessTaskTjeneste,
                                                   FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                                   ProsessTriggereRepository prosessTriggereRepository,
                                                   VilkårResultatRepository vilkårResultatRepository) {
        this.mottakere = mottakere;
        this.behandlingsoppretter = behandlingsoppretter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
        this.revurderingRepository = revurderingRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public void mottaDokument(Fagsak fagsak, Collection<MottattDokument> mottattDokument) {
        var brevkodeMap = mottattDokument
            .stream()
            .collect(Collectors.groupingBy(MottattDokument::getType));
        var triggere = getTriggere(mottattDokument, fagsak);

        var resultat = finnEllerOpprettBehandling(fagsak, triggere);

        ProsessTaskGruppe taskGruppe = new ProsessTaskGruppe();
        if (resultat.nyopprettet) {
            taskGruppe.addNesteSekvensiell(asynkStartBehandling(resultat.behandling));
        } else if (prosessenStårStillePåAksjonspunktForSøknadsfrist(resultat.behandling)) {
            taskGruppe = behandlingProsesseringTjeneste.opprettTaskGruppeForGjenopptaOppdaterFortsett(resultat.behandling, false, false, false);
        } else {
            taskGruppe = behandlingProsesseringTjeneste.opprettTaskGruppeForGjenopptaOppdaterFortsett(resultat.behandling, false, false);
        }

        // Må legges på etter at taskgruppe er satt opp for å få diff
        if (!triggere.isEmpty()) {
            prosessTriggereRepository.leggTil(resultat.behandling.getId(), triggere.stream().map(it -> new no.nav.ung.sak.trigger.Trigger(it.behandlingÅrsak(), it.periode())).collect(Collectors.toSet()));
        }
        lagreDokumenter(brevkodeMap, resultat.behandling);

        fjernBegrunnelsePåIkkevurdertVilkårsperiodeHvorOverlapperMedNySøknad(triggere, resultat.behandling);

        if (taskGruppe == null) {
            throw new IllegalStateException("Det er planlagt kjøringer som ikke har garantert rekkefølge. Sjekk oversikt over ventende tasker for eventuelt avbryte disse.");
        }

        // Lagrer tasks til slutt for å sikre at disse blir kjørt etter at dokumentasjon er lagret
        prosessTaskTjeneste.lagre(taskGruppe);
    }

    /**
     * Når saksbehandler innvilger korter enn 260 dager, ligger det i bakgrunnen en opprinnelig utledet periode som er inntil
     * ett år lang. Den delen som ikke blir innvilget, er funksjonelt sett ikke avslått heller, men vi har valgt å løse det
     * teknisk ved å lagre som avslag med en avslagsårsak som kan brukes for å se at dette er bare teknisk avslag. Den perioden
     * får også begrunnelse for hvorfor det ikke ble innvilget lenger.
     * <p>
     * Når det så kommer ny søknad som overlapper med den perioden som ikke ble innvilget, vil opprinnelig begrunnelse automatisk
     * bli kopiert over fra forrige behandling. Begrunnelsen er ikke relevant for vurdering av søknaden, så den fjernes her
     */
    private void fjernBegrunnelsePåIkkevurdertVilkårsperiodeHvorOverlapperMedNySøknad(List<Trigger> triggere, Behandling behandling) {
        DatoIntervallEntitet alltid = DatoIntervallEntitet.fraOgMedTilOgMed(AbstractLocalDateInterval.TIDENES_BEGYNNELSE, AbstractLocalDateInterval.TIDENES_ENDE);
        Optional<Behandling> forrigeBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(behandling.getFagsakId());
        if (forrigeBehandling.isEmpty()){
            return;
        }
        Vilkårene forrigeVilkårsresultat = vilkårResultatRepository.hent(forrigeBehandling.get().getId());
        Map<VilkårType, LocalDateTimeline<Boolean>> tekniskAvslagTidslinjer = forrigeVilkårsresultat.getVilkårTidslinjer(alltid).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e->e.getValue().filterValue(vp -> vp.getAvslagsårsak() == Avslagsårsak.AVKORTET).mapValue(_->true)));

        LocalDateTimeline<Boolean> nySøknadTidslinje = TidslinjeUtil.tilTidslinje(triggere.stream()
            .filter(t -> t.behandlingÅrsak() == BehandlingÅrsakType.NY_SØKT_PERIODE)
            .map(Trigger::periode)
            .toList());
        Vilkårene vilkårene = vilkårResultatRepository.hent(behandling.getId());
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        vilkårene.getVilkårTidslinjer(alltid)
            .forEach((vilkårType, vilkårTidslinje) -> {
                var tekniskAvslagTidslinje = tekniskAvslagTidslinjer.getOrDefault(vilkårType, LocalDateTimeline.empty());
                VilkårBuilder vilkårBuilder = builder.hentBuilderFor(vilkårType);
                vilkårTidslinje.intersection(nySøknadTidslinje).intersection(tekniskAvslagTidslinje).segmenter()
                    .forEach(segment -> {
                        if (segment.getValue().getUtfall() == Utfall.IKKE_VURDERT && segment.getValue().getBegrunnelse() != null) {
                            VilkårPeriodeBuilder vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(segment.getFom(), segment.getTom());
                            vilkårPeriodeBuilder.medBegrunnelse(null);
                            vilkårBuilder.leggTil(vilkårPeriodeBuilder);
                            log.info("Fjerner begrunnelse fra {} for periode {} til {} pga overlapp mot ny søknad", vilkårType, segment.getFom(), segment.getTom());
                        }
                    });
                builder.leggTil(vilkårBuilder);
            });
    }

    private boolean prosessenStårStillePåAksjonspunktForSøknadsfrist(Behandling behandling) {
        return BehandlingStegType.VURDER_SØKNADSFRIST.equals(behandling.getAktivtBehandlingSteg())
            && (behandling.getAksjonspunktForHvisFinnes(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE).map(Aksjonspunkt::erÅpentAksjonspunkt).orElse(false)
            || behandling.getAksjonspunktForHvisFinnes(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE).map(Aksjonspunkt::erÅpentAksjonspunkt).orElse(false));
    }

    private BehandlingMedOpprettelseResultat finnEllerOpprettBehandling(Fagsak fagsak, List<Trigger> triggere) {
        var fagsakId = fagsak.getId();
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteBehandling(fagsak.getId());

        if (sisteYtelsesbehandling.isEmpty()) {
            // ingen tidligere behandling - Opprett ny førstegangsbehandling
            log.info("Ingen tidligere behandling for fagsak {}, oppretter ny førstegangsbehandling", fagsakId);
            Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
            return BehandlingMedOpprettelseResultat.nyBehandling(behandling);
        } else {
            var sisteBehandling = sisteYtelsesbehandling.get();
            sjekkBehandlingKanLåses(sisteBehandling); // sjekker at kan låses (dvs ingen andre prosesserer den samtidig, hvis ikke kommer vi tilbake senere en gang)
            if (erBehandlingAvsluttet(sisteYtelsesbehandling)) {
                // siste behandling er avsluttet, oppretter ny behandling
                Optional<Behandling> sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsakId);
                sisteBehandling = sisteAvsluttetBehandling.orElse(sisteBehandling);

                // Håndter avsluttet behandling
                var sisteHenlagteFørstegangsbehandling = behandlingsoppretter.sisteHenlagteFørstegangsbehandling(sisteBehandling.getFagsak());
                if (sisteHenlagteFørstegangsbehandling.isPresent()) {
                    // oppretter ny behandling når siste var henlagt førstegangsbehandling
                    var nyFørstegangsbehandling = behandlingsoppretter.opprettNyFørstegangsbehandling(sisteHenlagteFørstegangsbehandling.get().getFagsak(), sisteHenlagteFørstegangsbehandling.get());
                    return BehandlingMedOpprettelseResultat.nyBehandling(nyFørstegangsbehandling);
                } else {
                    // oppretter ny behandling fra forrige (førstegangsbehandling eller revurdering)
                    var nyBehandling = behandlingsoppretter.opprettNyBehandlingFra(sisteBehandling, triggere.getFirst().behandlingÅrsak());
                    return BehandlingMedOpprettelseResultat.nyBehandling(nyBehandling);
                }
            } else {
                sjekkBehandlingKanHoppesTilbake(sisteBehandling);
                sjekkBehandlingHarIkkeÅpneTasks(sisteBehandling);
                return BehandlingMedOpprettelseResultat.eksisterendeBehandling(sisteBehandling);
            }
        }


    }

    public void lagreDokumenter(Map<Brevkode, List<MottattDokument>> mottattDokument, Behandling behandling) {
        mottattDokument.keySet()
            .stream()
            .sorted(Brevkode.COMP_REKKEFØLGE)
            .forEach(key -> {
                Dokumentmottaker dokumentmottaker = getDokumentmottaker(key, behandling.getFagsak());
                dokumentmottaker.lagreDokumentinnhold(mottattDokument.get(key), behandling);
            });
    }

    private List<Trigger> getTriggere(Collection<MottattDokument> mottatteDokumenter, Fagsak fagsak) {
        final var gruppertPåBrevkode = mottatteDokumenter.stream()
            .collect(Collectors.groupingBy(MottattDokument::getType));
        return gruppertPåBrevkode.entrySet().stream()
            .flatMap((entry) -> getDokumentmottaker(entry.getKey(), fagsak).getTriggere(entry.getValue()).stream())
            .toList();
    }

    private ProsessTaskData asynkStartBehandling(Behandling behandling) {
        return opprettTaskForÅStarteBehandling(behandling);
    }


    private ProsessTaskData opprettTaskForÅStarteBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData = ProsessTaskData.forProsessTask(StartBehandlingTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        return prosessTaskData;
    }

    private Boolean erBehandlingAvsluttet(Optional<Behandling> sisteYtelsesbehandling) {
        return sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.FALSE);
    }

    private void sjekkBehandlingKanLåses(Behandling behandling) {
        int forsøk = 15;

        BehandlingLås lås;
        while (--forsøk >= 0) {
            lås = behandlingLåsRepository.taLåsHvisLedig(behandling.getId());
            if (lås != null) {
                return; // OK - Fikk lås
            }
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // noen andre holder på siden vi ikke fikk fatt på lås, så avbryter denne gang
        throw DokumentmottakMidlertidigFeil.FACTORY.behandlingPågårAvventerKnytteMottattDokumentTilBehandling(behandling.getId()).toException();
    }

    private void sjekkBehandlingKanHoppesTilbake(Behandling behandling) {
        boolean underIverksetting = behandling.getStatus() == BehandlingStatus.IVERKSETTER_VEDTAK;
        if (underIverksetting) {
            //vedtak er fattet og behandlingen kan derfor ikke oppdateres. Må vente til behandlingen er avsluttet, og det vil så opprettes ny behandling når dokumentet sendes på nytt
            throw DokumentmottakMidlertidigFeil.FACTORY.behandlingUnderIverksettingAvventerKnytteMottattDokumentTilBehandling(behandling.getId()).toException();
        }
    }

    private void sjekkBehandlingHarIkkeÅpneTasks(Behandling behandling) {
        final Set<ProsessTaskStatus> aktuelleStatuser = EnumSet.of(ProsessTaskStatus.KLAR, ProsessTaskStatus.VENTER_SVAR, ProsessTaskStatus.VETO);
        final LocalDateTime fom = Tid.TIDENES_BEGYNNELSE.atStartOfDay();
        final LocalDateTime tom = Tid.TIDENES_ENDE.plusDays(1).atStartOfDay();
        //merk at denne bare finner tasks med gruppesekvensnummer != null (hindrer at den finner seg selv eller andre av typen innhentsaksopplysninger.håndterMottattDokument)
        final List<ProsessTaskData> åpneTasks = fagsakProsessTaskRepository.finnAlleForAngittSøk(behandling.getFagsakId(), behandling.getId().toString(), null, aktuelleStatuser, true);
        if (!åpneTasks.isEmpty()) {
            //behandlingen har åpne tasks og mottak av dokument kan føre til parallelle prosesser som går i beina på hverandre
            log.info("Fant følgende åpne tasks: [" + åpneTasks.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]");
            throw DokumentmottakMidlertidigFeil.FACTORY.behandlingPågårAvventerKnytteMottattDokumentTilBehandling(behandling.getId()).toException();
        }
    }

    private Dokumentmottaker getDokumentmottaker(Brevkode brevkode, Fagsak fagsak) {
        return finnMottaker(brevkode, fagsak.getYtelseType());
    }

    private Dokumentmottaker finnMottaker(Brevkode brevkode, FagsakYtelseType fagsakYtelseType) {
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(brevkode.getKode()));

        return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Dokumentmottaker for ytelseType=" + fagsakYtelseTypeKode + ", dokumentgruppe=" + brevkode));
    }

    record BehandlingMedOpprettelseResultat(Behandling behandling, boolean nyopprettet) {

        static BehandlingMedOpprettelseResultat nyBehandling(Behandling behandling) {
            return new BehandlingMedOpprettelseResultat(behandling, true);
        }

        static BehandlingMedOpprettelseResultat eksisterendeBehandling(Behandling behandling) {
            return new BehandlingMedOpprettelseResultat(behandling, false);
        }
    }
}
