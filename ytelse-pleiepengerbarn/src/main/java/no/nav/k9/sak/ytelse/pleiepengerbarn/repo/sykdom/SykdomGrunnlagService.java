package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomSamletVurdering;

@Dependent
public class SykdomGrunnlagService {

    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;


    SykdomGrunnlagService() {
        // CDI
    }

    @Inject
    public SykdomGrunnlagService(SykdomGrunnlagRepository sykdomGrunnlagRepository, BehandlingRepositoryProvider repositoryProvider) {
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    public SykdomGrunnlagBehandling hentGrunnlag(UUID behandlingUuid) {
        return sykdomGrunnlagRepository.hentGrunnlagForBehandling(behandlingUuid).orElseThrow();
    }

    boolean harDataSomIkkeHarBlittTattMedIBehandling(Behandling behandling) {
        final var resultat = utledRelevanteEndringerSidenForrigeGrunnlag(behandling);
        return resultat.harBlittEndret();
    }

    LocalDateTimeline<VilkårPeriode> hentOmsorgenForTidslinje(Long behandlingId) {
        final var vilkåreneOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        return vilkåreneOpt.map(v -> v.getVilkårTimeline(VilkårType.OMSORGEN_FOR)).orElse(LocalDateTimeline.empty());
    }

    LocalDateTimeline<VilkårPeriode> hentManglendeOmsorgenForTidslinje(Long behandlingId) {
        return hentOmsorgenForTidslinje(behandlingId).filterValue(v -> v.getUtfall() == Utfall.IKKE_OPPFYLT);
    }
    
    public List<Periode> hentManglendeOmsorgenForPerioder(Long behandlingId) {
        return SykdomUtils.toPeriodeList(hentManglendeOmsorgenForTidslinje(behandlingId));
    }

    public SykdomGrunnlagSammenlikningsresultat utledRelevanteEndringerSidenForrigeGrunnlag(Behandling behandling) {
        final Optional<SykdomGrunnlagBehandling> grunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(behandling.getUuid());

        final List<Periode> søknadsperioderSomSkalFjernes = hentManglendeOmsorgenForPerioder(behandling.getId());
        final SykdomGrunnlag utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(behandling.getFagsak().getSaksnummer(), behandling.getUuid(), behandling.getFagsak().getPleietrengendeAktørId(),
            grunnlagBehandling.map(bg -> bg.getGrunnlag().getSøktePerioder().stream().map(sp -> new Periode(sp.getFom(), sp.getTom())).collect(Collectors.toList())).orElse(List.of()),
            søknadsperioderSomSkalFjernes
            );

        return sammenlignGrunnlag(grunnlagBehandling.map(SykdomGrunnlagBehandling::getGrunnlag), utledetGrunnlag);
    }

    public SykdomGrunnlagSammenlikningsresultat utledRelevanteEndringerSidenForrigeBehandling(Behandling behandling, List<Periode> nyeVurderingsperioder) {
        final Optional<SykdomGrunnlagBehandling> forrigeGrunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagFraForrigeBehandling(behandling.getFagsak().getSaksnummer(), behandling.getUuid());
        final List<Periode> søknadsperioderSomSkalFjernes = hentManglendeOmsorgenForPerioder(behandling.getId());
        final SykdomGrunnlag utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(behandling.getFagsak().getSaksnummer(), behandling.getUuid(), behandling.getFagsak().getPleietrengendeAktørId(), nyeVurderingsperioder, søknadsperioderSomSkalFjernes);

        return sammenlignGrunnlag(forrigeGrunnlagBehandling.map(SykdomGrunnlagBehandling::getGrunnlag), utledetGrunnlag);
    }

    public SykdomGrunnlag utledGrunnlagMedManglendeOmsorgFjernet(Saksnummer saksnummer, UUID behandlingUuid, Long behandlingId, AktørId pleietrengende, List<Periode> vurderingsperioder) {
        final List<Periode> søknadsperioderSomSkalFjernes = hentManglendeOmsorgenForPerioder(behandlingId);
        return sykdomGrunnlagRepository.utledGrunnlag(saksnummer, behandlingUuid, pleietrengende, vurderingsperioder, søknadsperioderSomSkalFjernes);
    }

    public SykdomGrunnlagSammenlikningsresultat sammenlignGrunnlag(Optional<SykdomGrunnlag> forrigeGrunnlagBehandling, SykdomGrunnlag utledetGrunnlag) {
        boolean harEndretDiagnosekoder = sammenlignDiagnosekoder(forrigeGrunnlagBehandling, utledetGrunnlag);
        final LocalDateTimeline<Boolean> endringerISøktePerioder = sammenlignTidfestedeGrunnlagsdata(forrigeGrunnlagBehandling, utledetGrunnlag);
        return new SykdomGrunnlagSammenlikningsresultat(endringerISøktePerioder, harEndretDiagnosekoder);
    }

    LocalDateTimeline<Boolean> sammenlignTidfestedeGrunnlagsdata(Optional<SykdomGrunnlag> grunnlagBehandling, SykdomGrunnlag utledetGrunnlag) {
        LocalDateTimeline<SykdomSamletVurdering> grunnlagBehandlingTidslinje;

        if (grunnlagBehandling.isPresent()) {
            final SykdomGrunnlag forrigeGrunnlag = grunnlagBehandling.get();
            grunnlagBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(forrigeGrunnlag);
        } else {
            grunnlagBehandlingTidslinje = LocalDateTimeline.empty();
        }
        final LocalDateTimeline<SykdomSamletVurdering> forrigeGrunnlagTidslinje = grunnlagBehandlingTidslinje;

        final LocalDateTimeline<SykdomSamletVurdering> nyBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(utledetGrunnlag);
        final LocalDateTimeline<Boolean> endringerSidenForrigeBehandling = SykdomSamletVurdering.finnGrunnlagsforskjeller(forrigeGrunnlagTidslinje, nyBehandlingTidslinje);

        final LocalDateTimeline<Boolean> søktePerioderTimeline = SykdomUtils.toLocalDateTimeline(utledetGrunnlag.getSøktePerioder().stream().map(p -> new Periode(p.getFom(), p.getTom())).collect(Collectors.toList()));
        return endringerSidenForrigeBehandling.intersection(søktePerioderTimeline);
    }

    private boolean sammenlignDiagnosekoder(Optional<SykdomGrunnlag> grunnlagBehandling, SykdomGrunnlag utledetGrunnlag) {
        List<String> forrigeDiagnosekoder;
        if (grunnlagBehandling.isPresent()) {
            final SykdomGrunnlag forrigeGrunnlag = grunnlagBehandling.get();
            forrigeDiagnosekoder = forrigeGrunnlag.getSammenlignbarDiagnoseliste();
        } else {
            forrigeDiagnosekoder = Collections.emptyList();
        }
        final List<String> nyeDiagnosekoder = utledetGrunnlag.getSammenlignbarDiagnoseliste();

        return !forrigeDiagnosekoder.equals(nyeDiagnosekoder);
    }

}
