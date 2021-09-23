package no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBOppgittOpptjeningFilter implements OppgittOpptjeningFilter {


    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;

    PSBOppgittOpptjeningFilter() {
        // For CDI
    }

    @Inject
    public PSBOppgittOpptjeningFilter(VilkårTjeneste vilkårTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    /**
     * Henter sist mottatte oppgitte opptjening som kan knyttes til vilkårsperiode (vilkårsperiode slås opp gjennom stp)
     */
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));
        var vilkårsperiode = finnVilkårsperiodeForOpptjening(ref, stp);
        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravdokMedFravær = søknadsfristTjeneste.hentPerioderTilVurdering(ref);

        return finnOppgittOpptjening(iayGrunnlag, vilkårsperiode, kravdokMedFravær);
    }

    /**
     * Henter sist mottatte oppgitte opptjening som kan knyttes til vilkårsperiode
     */
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));
        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravdokMedFravær = søknadsfristTjeneste.hentPerioderTilVurdering(ref);

        return finnOppgittOpptjening(iayGrunnlag, vilkårsperiode, kravdokMedFravær);
    }


    Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode, Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær) {
        var stp = vilkårsperiode.getFomDato();
        var sistMottatteSøknadNærmestStp = kravDokumenterMedFravær.entrySet().stream()
            .min((e1, e2) -> {
                var kravdok1 = e1.getKey();
                var kravdok2 = e2.getKey();
                var søktePerioder1 = e1.getValue();
                var søktePerioder2 = e2.getValue();

                var compareStpDistanse = Long.compare(distanseTilStp(søktePerioder1, stp), distanseTilStp(søktePerioder2, stp));
                if (compareStpDistanse != 0) {
                    return compareStpDistanse;
                }
                return kravdok2.getInnsendingsTidspunkt().compareTo(kravdok1.getInnsendingsTidspunkt());
            })
            .orElseThrow();
        var journalpostId = sistMottatteSøknadNærmestStp.getKey().getJournalpostId();

        return finnOppgittOpptjening(iayGrunnlag, journalpostId);
    }

    private long distanseTilStp(List<SøktPeriode<Søknadsperiode>> søktePerioder, LocalDate skjæringstidspunkt) {
        var næresteDatoFraEksisterende = finnDatoNærmestSkjæringstidspunktet(søktePerioder, skjæringstidspunkt);
        long dist = Math.abs(ChronoUnit.DAYS.between(skjæringstidspunkt, næresteDatoFraEksisterende));
        return dist;
    }

    private LocalDate finnDatoNærmestSkjæringstidspunktet(List<SøktPeriode<Søknadsperiode>> søktePerioder, LocalDate stp) {
        var inkludert = søktePerioder.stream()
            .filter(p -> p.getPeriode().inkluderer(stp))
            .findFirst();
        if (inkludert.isPresent()) {
            return stp;
        }
        return søktePerioder.stream()
            .map(it -> it.getPeriode().getFomDato())
            .min(Comparator.comparingLong(x -> Math.abs(ChronoUnit.DAYS.between(stp, x))))
            .orElseThrow();
    }

    private DatoIntervallEntitet finnVilkårsperiodeForOpptjening(BehandlingReferanse ref, LocalDate stp) {
        var skalIgnorereAvslåttePerioder = false;
        var periodeTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, VilkårType.OPPTJENINGSVILKÅRET, skalIgnorereAvslåttePerioder)
            .stream()
            .filter(di -> di.getFomDato().equals(stp))
            .findFirst();
        if (periodeTilVurdering.isPresent()) {
            return periodeTilVurdering.get();
        }

        // Ingen match for stp mot perioder under vurdering -> må da forvente at den matcher vilkårsperiode som er ferdigvurdert
        var vilkår = vilkårTjeneste.hentVilkårResultat(ref.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow();
        var periodeFerdigvurdert = vilkår.finnPeriodeForSkjæringstidspunkt(stp);
        if (periodeFerdigvurdert.getGjeldendeUtfall().equals(Utfall.IKKE_VURDERT)) {
            throw new IllegalStateException("Forventer at vilkårsperiode som matchet opptjening var ferdigvurdert");
        }
        return periodeFerdigvurdert.getPeriode();
    }


    private Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag, JournalpostId sisteJournalpostId) {
        var oppgitteOpptjeninger = iayGrunnlag.getOppgittOpptjeningAggregat()
            .map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger)
            .orElse(List.of());
        return oppgitteOpptjeninger.stream()
            .filter(jp -> jp.getJournalpostId().equals(sisteJournalpostId))
            .findFirst();
    }

}
