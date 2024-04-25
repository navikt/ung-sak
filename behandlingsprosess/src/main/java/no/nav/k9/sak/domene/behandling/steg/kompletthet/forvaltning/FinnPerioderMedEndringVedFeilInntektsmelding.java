package no.nav.k9.sak.domene.behandling.steg.kompletthet.forvaltning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.kalkulus.felles.v1.Saksnummer;
import no.nav.folketrygdloven.kalkulus.request.v1.HentJournalpostIderRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.JournalpostIderResponse;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.ErEndringIRefusjonskravVurderer;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.InntektsmeldingerEndringsvurderer;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;

/**
 * Denne tjenesten finner perioder som påvirkes av at vi har brukt en annen inntektsmelding enn den siste prioriterte der dette også kan føre til endringer i utbetaling
 */
@ApplicationScoped
public class FinnPerioderMedEndringVedFeilInntektsmelding {

    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private KalkulusRestKlient kalkulusRestKlient;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private VilkårTjeneste vilkårTjeneste;
    private Instance<InntektsmeldingerEndringsvurderer> inntektsmeldingerEndringsvurderer;

    public FinnPerioderMedEndringVedFeilInntektsmelding() {
    }

    @Inject
    public FinnPerioderMedEndringVedFeilInntektsmelding(KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                                        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                        KalkulusRestKlient kalkulusRestKlient,
                                                        BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                        VilkårTjeneste vilkårTjeneste,
                                                        @Any Instance<InntektsmeldingerEndringsvurderer> inntektsmeldingerEndringsvurderer) {
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.kalkulusRestKlient = kalkulusRestKlient;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.inntektsmeldingerEndringsvurderer = inntektsmeldingerEndringsvurderer;
    }

    public Optional<RelevanteEndringer> finnPerioderForEndringDersomFeilInntektsmeldingBrukes(BehandlingReferanse behandlingReferanse, LocalDate fraDato) {

        var vilkårsperioder = finnBeregningVilkårsperioder(behandlingReferanse);
        var bgPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var alleInntektsmeldinger = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(behandlingReferanse.getSaksnummer(), behandlingReferanse.getAktørId(), behandlingReferanse.getFagsakYtelseType());

        if (alleInntektsmeldinger.stream().allMatch(it -> it.getMottattDato().isBefore(fraDato))) {
            return Optional.empty();
        }

        var inntektsmeldingerPrReferanse = finnInntektsmeldingerForBeregningPrEksternReferanse(behandlingReferanse, vilkårsperioder, bgPerioderGrunnlag, alleInntektsmeldinger);
        var journalpostIderResponses = finnJournalposterSomFaktiskErBruktIBeregning(behandlingReferanse, inntektsmeldingerPrReferanse);

        var relevanteEndringer = finnRelevanteEndringer(
            behandlingReferanse,
            journalpostIderResponses,
            inntektsmeldingerPrReferanse,
            alleInntektsmeldinger,
            bgPerioderGrunnlag,
            vilkårsperioder
        );

        if (!relevanteEndringer.vilkårsperioderTilRevurdering().isEmpty() || !relevanteEndringer.kunEndringIRefusjonListe.isEmpty()) {
            return Optional.of(relevanteEndringer);
        }

        return Optional.empty();


    }

    private RelevanteEndringer finnRelevanteEndringer(BehandlingReferanse behandlingReferanse, List<JournalpostIderResponse> journalpostIderResponses, Map<UUID, List<Inntektsmelding>> inntektsmeldingerPrReferanse, Set<Inntektsmelding> alleInntektsmeldinger, Optional<BeregningsgrunnlagPerioderGrunnlag> bgPerioderGrunnlag, List<DatoIntervallEntitet> vilkårsperioder) {
        List<DatoIntervallEntitet> vilkårsperioderForRevurdering = new ArrayList<>();
        List<DatoIntervallEntitet> kunEndringIRefusjonListe = new ArrayList<>();

        for (var jpresponse : journalpostIderResponses) {
            var eksternReferanse = jpresponse.getEksternReferanse();
            var inntektsmeldingerSomSkulleVærtBrukt = inntektsmeldingerPrReferanse.get(eksternReferanse);
            if (harEndring(jpresponse, inntektsmeldingerSomSkulleVærtBrukt)) {
                var inntektsmeldingerSomFaktiskErBrukt = finnInnteksmeldingerSomFaktiskErBrukt(jpresponse, alleInntektsmeldinger);
                var vilkårsperiode = finnVilkårsperiode(bgPerioderGrunnlag, eksternReferanse, vilkårsperioder);
                if (harEndringerSomPåvirkerVilkårsvurdering(behandlingReferanse, inntektsmeldingerSomSkulleVærtBrukt, inntektsmeldingerSomFaktiskErBrukt)) {
                    vilkårsperioderForRevurdering.add(vilkårsperiode);
                } else {
                    var endringIRefusjonTidslinje = ErEndringIRefusjonskravVurderer.finnEndringstidslinje(vilkårsperiode, inntektsmeldingerSomSkulleVærtBrukt, inntektsmeldingerSomFaktiskErBrukt);

                    if (!endringIRefusjonTidslinje.isEmpty()) {
                        var perioderMedEndringIRefusjon = endringIRefusjonTidslinje.getLocalDateIntervals()
                            .stream().map(DatoIntervallEntitet::fra)
                            .toList();
                        kunEndringIRefusjonListe.addAll(perioderMedEndringIRefusjon);
                    }
                }
            }

        }

        return new RelevanteEndringer(vilkårsperioderForRevurdering, kunEndringIRefusjonListe);
    }

    private List<DatoIntervallEntitet> finnBeregningVilkårsperioder(BehandlingReferanse behandlingReferanse) {
        var vilkårene = vilkårTjeneste.hentHvisEksisterer(behandlingReferanse.getBehandlingId());
        return vilkårene.flatMap(v -> v.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .map(VilkårPeriode::getPeriode)
            .toList();
    }

    private List<JournalpostIderResponse> finnJournalposterSomFaktiskErBruktIBeregning(BehandlingReferanse behandlingReferanse, Map<UUID, List<Inntektsmelding>> inntektsmeldingerPrReferanse) {
        return kalkulusRestKlient.hentInntektsmeldingJournalpostIder(new HentJournalpostIderRequest(inntektsmeldingerPrReferanse.keySet().stream().toList(), new Saksnummer(behandlingReferanse.getSaksnummer().getVerdi())));
    }

    private Map<UUID, List<Inntektsmelding>> finnInntektsmeldingerForBeregningPrEksternReferanse(BehandlingReferanse behandlingReferanse, List<DatoIntervallEntitet> vilkårsperioder, Optional<BeregningsgrunnlagPerioderGrunnlag> bgPerioderGrunnlag, Set<Inntektsmelding> alleInntektsmeldinger) {
        return vilkårsperioder.stream()
            .filter(p -> bgPerioderGrunnlag.flatMap(gr -> gr.finnGrunnlagFor(p.getFomDato())).isPresent())
            .collect(Collectors.toMap(
                p -> bgPerioderGrunnlag.flatMap(gr -> gr.finnGrunnlagFor(p.getFomDato())).map(BeregningsgrunnlagPeriode::getEksternReferanse).orElseThrow(),
                p -> kompletthetForBeregningTjeneste.utledInntektsmeldingerSomSendesInnTilBeregningForPeriode(behandlingReferanse, alleInntektsmeldinger, p)
            ));
    }

    private static Set<Inntektsmelding> finnInnteksmeldingerSomFaktiskErBrukt(JournalpostIderResponse jpresponse, Set<Inntektsmelding> alleInntektsmeldinger) {
        var journalpostIderSomFaktiskErBrukt = jpresponse.getJournalpostIder().stream().map(no.nav.folketrygdloven.kalkulus.felles.v1.JournalpostId::getId).collect(Collectors.toSet());
        return finnInntektsmeldingerFraJournalspostId(journalpostIderSomFaktiskErBrukt, alleInntektsmeldinger);
    }

    private static boolean harEndring(JournalpostIderResponse jpresponse, List<Inntektsmelding> inntektsmeldingerSomSkulleVærtBrukt) {
        var jounalpostIderSomSkulleVærtBrukt = inntektsmeldingerSomSkulleVærtBrukt.stream().map(Inntektsmelding::getJournalpostId).map(JournalpostId::getVerdi).collect(Collectors.toSet());
        var journalpostIderSomFaktiskErBrukt = jpresponse.getJournalpostIder().stream().map(no.nav.folketrygdloven.kalkulus.felles.v1.JournalpostId::getId).collect(Collectors.toSet());
        return !erLike(journalpostIderSomFaktiskErBrukt, jounalpostIderSomSkulleVærtBrukt);
    }

    private static Set<Inntektsmelding> finnInntektsmeldingerFraJournalspostId(Set<String> journalpostIderSomFaktiskErBrukt, Set<Inntektsmelding> alleInntektsmeldinger) {
        return journalpostIderSomFaktiskErBrukt.stream()
            .flatMap(id -> alleInntektsmeldinger.stream().filter(it -> it.getJournalpostId().getVerdi().equals(id)))
            .collect(Collectors.toSet());
    }

    private boolean harEndringerSomPåvirkerVilkårsvurdering(BehandlingReferanse behandlingReferanse, List<Inntektsmelding> inntektsmeldingerSomSkulleVærtBrukt, Set<Inntektsmelding> inntektsmeldingerSomFaktiskErBrukt) {
        var inntektsmeldingerMedRelevanteEndringerForVilkårsvurdering = InntektsmeldingerEndringsvurderer.finnTjeneste(inntektsmeldingerEndringsvurderer, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, behandlingReferanse.getFagsakYtelseType()).finnInntektsmeldingerMedRelevanteEndringer(inntektsmeldingerSomSkulleVærtBrukt, inntektsmeldingerSomFaktiskErBrukt);
        return !inntektsmeldingerMedRelevanteEndringerForVilkårsvurdering.isEmpty();
    }

    private static DatoIntervallEntitet finnVilkårsperiode(Optional<BeregningsgrunnlagPerioderGrunnlag> bgPerioderGrunnlag, UUID eksternReferanse, List<DatoIntervallEntitet> vilkårsperioder) {
        return bgPerioderGrunnlag.stream()
            .flatMap(bgp -> bgp.getGrunnlagPerioder().stream().filter(it -> it.getEksternReferanse().equals(eksternReferanse)))
            .map(BeregningsgrunnlagPeriode::getSkjæringstidspunkt)
            .flatMap(stp -> vilkårsperioder.stream().filter(p1 -> p1.getFomDato().equals(stp)))
            .findFirst().orElseThrow();
    }

    private static boolean erLike(Set<String> journalpostIderSomErBrukt, Set<String> jounalpostIderSomSkulleVærtBrukt) {
        return journalpostIderSomErBrukt.size() == jounalpostIderSomSkulleVærtBrukt.size() && journalpostIderSomErBrukt.containsAll(jounalpostIderSomSkulleVærtBrukt);
    }

    public record RelevanteEndringer(List<DatoIntervallEntitet> vilkårsperioderTilRevurdering,
                                     List<DatoIntervallEntitet> kunEndringIRefusjonListe) {

    }


}
