package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.tilsyn.Kilde;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.delt.UtledetEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak.EtablertTilsynRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.Tilsynsordning;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@Dependent
public class EtablertTilsynTjeneste {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    private EtablertTilsynRepository etablertTilsynRepository;

    @Inject
    public EtablertTilsynTjeneste(FagsakRepository fagsakRepository,
                                  BehandlingRepository behandlingRepository,
                                  UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                  @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjenester,
                                  EtablertTilsynRepository etablertTilsynRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.søknadsfristTjeneste = søknadsfristTjenester;
        this.etablertTilsynRepository = etablertTilsynRepository;
    }


    public LocalDateTimeline<UtledetEtablertTilsyn> beregnTilsynstidlinje(BehandlingReferanse behandlingRef) {
        final var tilsynsgrunnlagPåTversAvFagsaker = hentAllePerioderTilVurdering(behandlingRef.getPleietrengendeAktørId(), behandlingRef.getFagsakPeriode());
        return byggTidslinje(behandlingRef.getSaksnummer(), tilsynsgrunnlagPåTversAvFagsaker);
    }
    
    public void opprettGrunnlagForTilsynstidlinje(BehandlingReferanse behandlingRef) {
        final LocalDateTimeline<UtledetEtablertTilsyn> tilsynstidslinje = beregnTilsynstidlinje(behandlingRef);
        
        final List<EtablertTilsynPeriode> tilsynsperioder = tilsynstidslinje.stream()
            .map(s -> new EtablertTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue().getVarighet(), s.getValue().getJournalpostId()))
            .collect(Collectors.toList());
        
        final EtablertTilsyn etablertTilsyn = new EtablertTilsyn(tilsynsperioder);
        etablertTilsynRepository.lagre(behandlingRef.getBehandlingId(), etablertTilsyn);
    }


    private List<FagsakKravDokument> hentAllePerioderTilVurdering(AktørId pleietrengende, DatoIntervallEntitet fagsakPeriode) {
        final List<Fagsak> fagsaker = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, pleietrengende, null, fagsakPeriode.getFomDato().minusWeeks(25), fagsakPeriode.getTomDato().plusWeeks(25));

        final List<FagsakKravDokument> kravdokumenter = new ArrayList<>();
        for (Fagsak f : fagsaker) {
            final Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(f.getId());
            if (behandlingOpt.isEmpty()) {
                continue;
            }
            final var behandling = behandlingOpt.get();
            final var behandlingReferanse = BehandlingReferanse.fra(behandling);

            final var uttakGrunnlagOpt = uttakPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
            if (uttakGrunnlagOpt.isEmpty() || uttakGrunnlagOpt.get().getOppgitteSøknadsperioder() == null) {
                continue;
            }

            final Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravdokumentListe = søknadsfristTjeneste.hentPerioderTilVurdering(behandlingReferanse);
            final Set<PerioderFraSøknad> fagsakPerioderFraSøknadene = uttakGrunnlagOpt.get().getOppgitteSøknadsperioder().getPerioderFraSøknadene();

            final List<FagsakKravDokument> fagsakKravdokumenter = toFagsakKravDokumenter(f, kravdokumentListe, fagsakPerioderFraSøknadene);
            kravdokumenter.addAll(fagsakKravdokumenter);
        }

        Collections.sort(kravdokumenter);
        return kravdokumenter;
    }

    private List<FagsakKravDokument> toFagsakKravDokumenter(Fagsak f, Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravdokumentListe, Set<PerioderFraSøknad> fagsakPerioderFraSøknadene) {
        return kravdokumentListe.keySet().stream().map(kd -> new FagsakKravDokument(f, kd, finnPerioderFraSøknad(kd, fagsakPerioderFraSøknadene))).collect(Collectors.toList());
    }

    private PerioderFraSøknad finnPerioderFraSøknad(KravDokument kravDokument, Set<PerioderFraSøknad> fagsakPerioderFraSøknadene) {
        final var dokumenter = fagsakPerioderFraSøknadene.stream()
            .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
            .collect(Collectors.toSet());
        if (dokumenter.size() != 1) {
            throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + dokumenter);
        }
        return dokumenter.iterator().next();
    }

    private LocalDateTimeline<UtledetEtablertTilsyn> byggTidslinje(Saksnummer søkersSaksnummer, List<FagsakKravDokument> fagsakKravDokumenter) {
        var resultatTimeline = new LocalDateTimeline<UtledetEtablertTilsyn>(List.of());
        for (FagsakKravDokument kravDokument : fagsakKravDokumenter) {
            for (var periode : kravDokument.perioderFraSøknad.getTilsynsordning().stream().map(Tilsynsordning::getPerioder).flatMap(Collection::stream).collect(Collectors.toList())) {
                final var kilde = søkersSaksnummer.equals(kravDokument.fagsak.getSaksnummer()) ? Kilde.SØKER : Kilde.ANDRE;
                final var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), new UtledetEtablertTilsyn(periode.getVarighet(), kilde, kravDokument.kravDokument.getJournalpostId()))));
                resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
        }
        return resultatTimeline.compress();
    }

    private static final class FagsakKravDokument implements Comparable<FagsakKravDokument> {
        private final Fagsak fagsak;
        private final KravDokument kravDokument;
        private final PerioderFraSøknad perioderFraSøknad;

        public FagsakKravDokument(Fagsak fagsak, KravDokument kravDokument, PerioderFraSøknad perioderFraSøknad) {
            this.fagsak = fagsak;
            this.kravDokument = kravDokument;
            this.perioderFraSøknad = perioderFraSøknad;
        }

        @Override
        public int compareTo(FagsakKravDokument o) {
            return kravDokument.compareTo(o.kravDokument);
        }
    }
}
