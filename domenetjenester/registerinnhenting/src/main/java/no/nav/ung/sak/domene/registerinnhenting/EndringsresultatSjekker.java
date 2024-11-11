package no.nav.ung.sak.domene.registerinnhenting;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;

@Dependent
public class EndringsresultatSjekker {

    private static final Logger log = LoggerFactory.getLogger(EndringsresultatSjekker.class);

    private PersonopplysningTjeneste personopplysningTjeneste;
    private Instance<SøknadDokumentTjeneste> søknadsDokumentTjenester;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    private ProsessTriggereRepository prosessTriggereRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<InformasjonselementerUtleder> informasjonselementer;
    private BehandlingRepository behandlingRepository;

    private Instance<EndringStartpunktUtleder> startpunktUtledere;

    EndringsresultatSjekker() {
        // For CDI
    }

    @Inject
    public EndringsresultatSjekker(PersonopplysningTjeneste personopplysningTjeneste,
                                   @Any Instance<InformasjonselementerUtleder> informasjonselementer,
                                   @Any Instance<EndringStartpunktUtleder> startpunktUtledere,
                                   @Any Instance<SøknadDokumentTjeneste> søknadsDokumentTjenester,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   ProsessTriggereRepository prosessTriggereRepository,
                                   BehandlingRepositoryProvider provider) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.informasjonselementer = informasjonselementer;
        this.startpunktUtledere = startpunktUtledere;
        this.søknadsDokumentTjenester = søknadsDokumentTjenester;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.vilkårResultatRepository = provider.getVilkårResultatRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
    }

    static Long mapFraLocalDateTimeTilLong(LocalDateTime ldt) {
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("Europe/Paris"));
        return zdt.toInstant().toEpochMilli();
    }

    public EndringsresultatSnapshot opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(Long behandlingId) {
        EndringsresultatSnapshot snapshot = EndringsresultatSnapshot.opprett();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        snapshot.leggTil(personopplysningTjeneste.finnAktivGrunnlagId(behandlingId));
        snapshot.leggTil(prosessTriggereRepository.finnAktivGrunnlagId(behandlingId)); // annen part etc

        if (inkludererBeregning(behandling)) {
            EndringsresultatSnapshot iaySnapshot = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId)
                .map(iayg -> EndringsresultatSnapshot.medSnapshot(InntektArbeidYtelseGrunnlag.class, iayg.getEksternReferanse()))
                .orElse(EndringsresultatSnapshot.utenSnapshot(InntektArbeidYtelseGrunnlag.class));
            snapshot.leggTil(iaySnapshot);
        }

        var søknadDokumentTjeneste = SøknadDokumentTjeneste.finnTjeneste(søknadsDokumentTjenester, behandling.getFagsakYtelseType());
        if (søknadDokumentTjeneste.isPresent()) {
            snapshot.leggTil(søknadDokumentTjeneste.get().finnAktivGrunnlagId(behandlingId));
        } else {
            log.info("Fant ingen søknadsdokument endringssjekker for ytelse {}", behandling.getFagsakYtelseType());
        }
        var utvidetBehandlingsgrunnlagTjeneste = DiffUtvidetBehandlingsgrunnlagTjeneste.finnTjeneste(behandling.getFagsakYtelseType());
        utvidetBehandlingsgrunnlagTjeneste.ifPresent(diffUtvidetBehandlingsgrunnlagTjeneste -> diffUtvidetBehandlingsgrunnlagTjeneste.leggTilSnapshot(BehandlingReferanse.fra(behandling), snapshot));

        if (utvidetBehandlingsgrunnlagTjeneste.isEmpty()) {
            log.info("Fant ingen utvidetbehandlingsgrunnlagtjeneste for ytelse {}", behandling.getFagsakYtelseType());
        }

        return snapshot;
    }

    public boolean harStatusOpprettetEllerUtredes(Behandling behandling) {
        return Set.of(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES).contains(behandling.getStatus());
    }

    private boolean inkludererBeregning(Behandling behandling) {
        return !(finnTjeneste(behandling.getFagsakYtelseType(), behandling.getType()).utled(behandling.getType()).isEmpty());
    }

    public EndringsresultatDiff finnSporedeEndringerPåBehandlingsgrunnlag(Long behandlingId, EndringsresultatSnapshot snapshotFør) {
        final boolean kunSporedeEndringer = true;
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        // Del 1: Finn diff mellom grunnlagets id før og etter oppdatering
        EndringsresultatSnapshot idSnapshotNå = opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId);
        EndringsresultatDiff idDiff = idSnapshotNå.minus(snapshotFør);

        // Del 2: Transformer diff på grunnlagets id til diff på grunnlagets sporede endringer (@ChangeTracked)
        EndringsresultatDiff sporedeEndringerDiff = EndringsresultatDiff.opprettForSporingsendringer();

        EndringStartpunktUtleder.finnUtleder(startpunktUtledere, PersonInformasjonEntitet.class, behandling.getFagsakYtelseType()).ifPresent(u -> {
            idDiff.hentDelresultat(PersonInformasjonEntitet.class)
                .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> personopplysningTjeneste.diffResultat(idEndring, kunSporedeEndringer)));
        });


        EndringStartpunktUtleder.finnUtleder(startpunktUtledere, ProsessTriggere.class, behandling.getFagsakYtelseType())
            .flatMap(u -> idDiff.hentDelresultat(ProsessTriggere.class))
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> prosessTriggereRepository.diffResultat(idEndring, kunSporedeEndringer)));

        EndringStartpunktUtleder.finnUtleder(startpunktUtledere, InntektArbeidYtelseGrunnlag.class, behandling.getFagsakYtelseType()).ifPresent(u -> {
            if (inkludererBeregning(behandling)) {
                idDiff.hentDelresultat(InntektArbeidYtelseGrunnlag.class).ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> {
                    InntektArbeidYtelseGrunnlag grunnlag1 = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, (UUID) idEndring.getGrunnlagId1());
                    InntektArbeidYtelseGrunnlag grunnlag2 = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, (UUID) idEndring.getGrunnlagId2());
                    return new IAYGrunnlagDiff(grunnlag1, grunnlag2).diffResultat(kunSporedeEndringer);
                }));
            }
        });

        var søknadDokumentTjeneste = SøknadDokumentTjeneste.finnTjeneste(søknadsDokumentTjenester, behandling.getFagsakYtelseType());
        søknadDokumentTjeneste.ifPresent(dokumentTjeneste -> idDiff.hentDelresultat(dokumentTjeneste.getGrunnlagsKlasse())
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> dokumentTjeneste.diffResultat(idEndring, kunSporedeEndringer))));

        var utvidetBehandlingsgrunnlagTjeneste = DiffUtvidetBehandlingsgrunnlagTjeneste.finnTjeneste(behandling.getFagsakYtelseType());
        utvidetBehandlingsgrunnlagTjeneste.ifPresent(diffUtvidetBehandlingsgrunnlagTjeneste -> diffUtvidetBehandlingsgrunnlagTjeneste.leggTilDiffResultat(BehandlingReferanse.fra(behandling), idDiff, sporedeEndringerDiff));

        return sporedeEndringerDiff;
    }

    public EndringsresultatSnapshot opprettEndringsresultatIdPåBehandlingSnapshot(Behandling behandling) {
        Long behandlingId = behandling.getId();
        EndringsresultatSnapshot snapshot = opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId);


        // Resultatstrukturene nedenfor støtter ikke paradigme med "aktivt" grunnlag som kan identifisere med id
        // Aksepterer her at endringssjekk heller utledes av deres tidsstempel forutsatt at metoden ikke brukes i
        // kritiske endringssjekker. Håp om at de i fremtiden vil støtte paradigme.
        snapshot.leggTil(lagVilkårResultatIdSnapshotAvTidsstempel(behandling));

        return snapshot;
    }

    public EndringsresultatDiff finnIdEndringerPåBehandling(Behandling behandling, EndringsresultatSnapshot idSnapshotFør) {
        EndringsresultatSnapshot idSnapshotNå = opprettEndringsresultatIdPåBehandlingSnapshot(behandling);
        return idSnapshotNå.minus(idSnapshotFør);
    }

    private EndringsresultatSnapshot lagVilkårResultatIdSnapshotAvTidsstempel(Behandling behandling) {
        return vilkårResultatRepository.hentHvisEksisterer(behandling.getId())
            .map(vilkårResultat -> EndringsresultatSnapshot.medSnapshot(Vilkårene.class, hentLongVerdiAvEndretTid(vilkårResultat)))
            .orElse(EndringsresultatSnapshot.utenSnapshot(Vilkårene.class));
    }

    private Long hentLongVerdiAvEndretTid(BaseEntitet entitet) {
        LocalDateTime endretTidspunkt = entitet.getOpprettetTidspunkt();
        if (entitet.getEndretTidspunkt() != null) {
            endretTidspunkt = entitet.getEndretTidspunkt();
        }
        return mapFraLocalDateTimeTilLong(endretTidspunkt);
    }

    private InformasjonselementerUtleder finnTjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return InformasjonselementerUtleder.finnTjeneste(informasjonselementer, ytelseType, behandlingType);
    }
}
