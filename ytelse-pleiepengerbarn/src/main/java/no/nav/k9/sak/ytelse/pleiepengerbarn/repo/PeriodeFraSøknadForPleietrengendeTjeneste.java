package no.nav.k9.sak.ytelse.pleiepengerbarn.repo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@Dependent
public class PeriodeFraSøknadForPleietrengendeTjeneste {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;

    @Inject
    public PeriodeFraSøknadForPleietrengendeTjeneste(FagsakRepository fagsakRepository,
                                                     BehandlingRepository behandlingRepository,
                                                     UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                                     @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjenester) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.søknadsfristTjeneste = søknadsfristTjenester;
    }


    public List<FagsakKravDokument> hentAllePerioderTilVurdering(AktørId pleietrengende, DatoIntervallEntitet fagsakPeriode) {
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

    public static final class FagsakKravDokument implements Comparable<FagsakKravDokument> {
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

        public Fagsak getFagsak() {
            return fagsak;
        }

        public KravDokument getKravDokument() {
            return kravDokument;
        }

        public PerioderFraSøknad getPerioderFraSøknad() {
            return perioderFraSøknad;
        }
    }
}
