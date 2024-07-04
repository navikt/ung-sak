package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomSamletVurdering;

@Dependent
public class MedisinskGrunnlagTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;


    MedisinskGrunnlagTjeneste() {
        // CDI
    }

    @Inject
    public MedisinskGrunnlagTjeneste(MedisinskGrunnlagRepository medisinskGrunnlagRepository, BehandlingRepositoryProvider repositoryProvider) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    public MedisinskGrunnlag hentGrunnlag(UUID behandlingUuid) {
        return medisinskGrunnlagRepository.hentGrunnlagForBehandling(behandlingUuid).orElseThrow();
    }

    public Optional<MedisinskGrunnlag> hentGrunnlagHvisEksisterer(UUID behandlingUuid) {
        return medisinskGrunnlagRepository.hentGrunnlagForBehandling(behandlingUuid);
    }

    public boolean harDataSomIkkeHarBlittTattMedIBehandling(Behandling behandling) {
        final var resultat = utledRelevanteEndringerSidenForrigeGrunnlag(behandling);
        return resultat.harBlittEndret();
    }

    public LocalDateTimeline<VilkårPeriode> hentOmsorgenForTidslinje(Long behandlingId) {
        final var vilkåreneOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        return vilkåreneOpt.map(v -> v.getVilkårTimeline(VilkårType.OMSORGEN_FOR)).orElse(LocalDateTimeline.empty());
    }

    public LocalDateTimeline<VilkårPeriode> hentManglendeOmsorgenForTidslinje(Long behandlingId) {
        return hentOmsorgenForTidslinje(behandlingId).filterValue(v -> v.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT);
    }

    public NavigableSet<DatoIntervallEntitet> hentManglendeOmsorgenForPerioder(Long behandlingId) {
        return TidslinjeUtil.tilDatoIntervallEntiteter(hentManglendeOmsorgenForTidslinje(behandlingId));
    }

    public SykdomGrunnlagSammenlikningsresultat utledRelevanteEndringerSidenForrigeGrunnlag(Behandling behandling) {
        final Optional<MedisinskGrunnlag> grunnlagBehandling = medisinskGrunnlagRepository.hentGrunnlagForBehandling(behandling.getUuid());

        final NavigableSet<DatoIntervallEntitet> søknadsperioderSomSkalFjernes = hentManglendeOmsorgenForPerioder(behandling.getId());
        final MedisinskGrunnlagsdata utledetGrunnlag = medisinskGrunnlagRepository.utledGrunnlag(behandling.getFagsak().getSaksnummer(), behandling.getUuid(), behandling.getFagsak().getPleietrengendeAktørId(),
            grunnlagBehandling.map(bg -> bg.getGrunnlagsdata().getSøktePerioder().stream().map(sp -> DatoIntervallEntitet.fraOgMedTilOgMed(sp.getFom(), sp.getTom())).collect(Collectors.toCollection(TreeSet::new))).orElse(new TreeSet<>()),
            søknadsperioderSomSkalFjernes
            );

        return sammenlignGrunnlag(grunnlagBehandling.map(MedisinskGrunnlag::getGrunnlagsdata), utledetGrunnlag, true);
    }

    public SykdomGrunnlagSammenlikningsresultat utledRelevanteEndringerSidenForrigeBehandling(Behandling behandling, NavigableSet<DatoIntervallEntitet> nyeVurderingsperioder) {
        final Optional<MedisinskGrunnlag> forrigeGrunnlagBehandling = medisinskGrunnlagRepository.hentGrunnlagFraForrigeBehandling(behandling.getFagsak().getSaksnummer(), behandling.getUuid());
        final NavigableSet<DatoIntervallEntitet> søknadsperioderSomSkalFjernes = hentManglendeOmsorgenForPerioder(behandling.getId());
        final MedisinskGrunnlagsdata utledetGrunnlag = medisinskGrunnlagRepository.utledGrunnlag(behandling.getFagsak().getSaksnummer(), behandling.getUuid(), behandling.getFagsak().getPleietrengendeAktørId(), nyeVurderingsperioder, søknadsperioderSomSkalFjernes);

        return sammenlignGrunnlag(forrigeGrunnlagBehandling.map(MedisinskGrunnlag::getGrunnlagsdata), utledetGrunnlag, false);
    }

    public MedisinskGrunnlagsdata utledGrunnlagMedManglendeOmsorgFjernet(Saksnummer saksnummer, UUID behandlingUuid, Long behandlingId, AktørId pleietrengende, NavigableSet<DatoIntervallEntitet> vurderingsperioder) {
        final NavigableSet<DatoIntervallEntitet> søknadsperioderSomSkalFjernes = hentManglendeOmsorgenForPerioder(behandlingId);
        return medisinskGrunnlagRepository.utledGrunnlag(saksnummer, behandlingUuid, pleietrengende, vurderingsperioder, søknadsperioderSomSkalFjernes);
    }

    public SykdomGrunnlagSammenlikningsresultat sammenlignGrunnlag(Optional<MedisinskGrunnlagsdata> forrigeGrunnlagBehandling, MedisinskGrunnlagsdata utledetGrunnlag, boolean skalBrukeInnleggelse) {
        boolean harEndretDiagnosekoder = sammenlignDiagnosekoder(forrigeGrunnlagBehandling, utledetGrunnlag);
        boolean harNyeUklassifiserteDokumenter = harNyeUklassifiserteDokumenter(forrigeGrunnlagBehandling, utledetGrunnlag);
        final LocalDateTimeline<Boolean> endringerISøktePerioder = sammenlignTidfestedeGrunnlagsdata(forrigeGrunnlagBehandling, utledetGrunnlag, skalBrukeInnleggelse);
        return new SykdomGrunnlagSammenlikningsresultat(endringerISøktePerioder, harEndretDiagnosekoder, harNyeUklassifiserteDokumenter);
    }

    private boolean harNyeUklassifiserteDokumenter(Optional<MedisinskGrunnlagsdata> forrigeGrunnlagBehandlingOpt, MedisinskGrunnlagsdata utledetGrunnlag) {
        if (forrigeGrunnlagBehandlingOpt.isEmpty()) {
            return false;
        }

        var forrige = forrigeGrunnlagBehandlingOpt.get().getSykdomsdokumenter().stream()
            .filter(it -> it.getType() == SykdomDokumentType.UKLASSIFISERT)
            .map(it -> new PleietrengendeDokumentID(it.getJournalpostId().getVerdi(), it.getDokumentInfoId()))
            .collect(Collectors.toSet());

        var diffSet = utledetGrunnlag.getSykdomsdokumenter().stream()
            .filter(it -> it.getType() == SykdomDokumentType.UKLASSIFISERT)
            .map(it -> new PleietrengendeDokumentID(it.getJournalpostId().getVerdi(), it.getDokumentInfoId()))
            .collect(Collectors.toCollection(HashSet::new));

        diffSet.removeAll(forrige);
        return !diffSet.isEmpty();

    }

    private record PleietrengendeDokumentID(String journalpostId, String dokumentId) {}


    LocalDateTimeline<Boolean> sammenlignTidfestedeGrunnlagsdata(Optional<MedisinskGrunnlagsdata> grunnlagBehandling, MedisinskGrunnlagsdata utledetGrunnlag, boolean skalBrukeInnleggelse) {
        LocalDateTimeline<SykdomSamletVurdering> grunnlagBehandlingTidslinje;

        if (grunnlagBehandling.isPresent()) {
            final MedisinskGrunnlagsdata forrigeGrunnlag = grunnlagBehandling.get();
            grunnlagBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(forrigeGrunnlag, skalBrukeInnleggelse);
        } else {
            grunnlagBehandlingTidslinje = LocalDateTimeline.empty();
        }
        final LocalDateTimeline<SykdomSamletVurdering> forrigeGrunnlagTidslinje = grunnlagBehandlingTidslinje;

        final LocalDateTimeline<SykdomSamletVurdering> nyBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(utledetGrunnlag, skalBrukeInnleggelse);
        final LocalDateTimeline<Boolean> endringerSidenForrigeBehandling = SykdomSamletVurdering.finnGrunnlagsforskjeller(forrigeGrunnlagTidslinje, nyBehandlingTidslinje);

        final LocalDateTimeline<Boolean> søktePerioderTimeline = TidslinjeUtil.tilTidslinjeKomprimert(utledetGrunnlag.getSøktePerioder().stream().map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom())).collect(Collectors.toCollection(TreeSet::new)));
        return endringerSidenForrigeBehandling.intersection(søktePerioderTimeline);
    }

    private boolean sammenlignDiagnosekoder(Optional<MedisinskGrunnlagsdata> grunnlagBehandling, MedisinskGrunnlagsdata utledetGrunnlag) {
        List<String> forrigeDiagnosekoder;
        if (grunnlagBehandling.isPresent()) {
            final MedisinskGrunnlagsdata forrigeGrunnlag = grunnlagBehandling.get();
            forrigeDiagnosekoder = forrigeGrunnlag.getSammenlignbarDiagnoseliste();
        } else {
            forrigeDiagnosekoder = Collections.emptyList();
        }
        final List<String> nyeDiagnosekoder = utledetGrunnlag.getSammenlignbarDiagnoseliste();

        return !forrigeDiagnosekoder.equals(nyeDiagnosekoder);
    }

}
