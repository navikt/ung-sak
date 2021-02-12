package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Barn;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Pleiebehov;
import no.nav.pleiepengerbarn.uttak.kontrakter.Søker;
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Vilkårsperiode;

@Dependent
public class MapInputTilUttakTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private UttakRepository uttakRepository;
    private BehandlingRepository behandlingRepository;


    @Inject
    public MapInputTilUttakTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                    PleiebehovResultatRepository pleiebehovResultatRepository,
                                    UttakRepository uttakRepository,
                                    BehandlingRepository behandlingRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.uttakRepository = uttakRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public Uttaksgrunnlag hentUtOgMapRequest(BehandlingReferanse referanse) {       
        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var uttakGrunnlag = uttakRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();
        var oppgittUttak = uttakGrunnlag.getOppgittUttak();
        var pleiebehov = pleiebehovResultatRepository.hent(referanse.getBehandlingId());
        
        return toRequestData(behandling, vilkårene, oppgittUttak, pleiebehov);
    }

    private Uttaksgrunnlag toRequestData(Behandling behandling, Vilkårene vilkårene, UttakAktivitet oppgittUttak, PleiebehovResultat pleiebehov) {
        // TODO: Dødsdato:
        final Barn barn = new Barn(behandling.getFagsak().getPleietrengendeAktørId().getId(), null);
        
        // TODO: Fødsels- og dødsdato:
        final Søker søker = new Søker(behandling.getAktørId().getId(), LocalDate.now().minusYears(20), null); 
        
        // TODO: Map:
        final List<String> andrePartersSaksnummer = List.of();
        
        // TODO: Sett faktisk søkt uttak:
        final List<SøktUttak> søktUttak = oppgittUttak.getPerioder()
                .stream()
                .map(uap -> new SøktUttak(toLukketPeriode(uap.getPeriode()), null))
                .collect(Collectors.toList());
        
        // TODO: Se kommentarer/TODOs under denne:
        final List<Arbeid> arbeid = toArbeid(oppgittUttak);
        
        final Map<LukketPeriode, Pleiebehov> tilsynsbehov = toTilsynsbehov(pleiebehov);
        
        // TODO: Map:
        final List<LukketPeriode> lovbestemtFerie = List.of();
        
        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = toInngangsvilkår(vilkårene);
        
        // TODO: Map:
        final Map<LukketPeriode, Duration> tilsynsperioder = Map.of();
        
        return new Uttaksgrunnlag(
                barn,
                søker,
                behandling.getFagsak().getSaksnummer().getVerdi(),
                behandling.getUuid().toString(),
                andrePartersSaksnummer,
                søktUttak,
                arbeid,
                tilsynsbehov,
                lovbestemtFerie,
                inngangsvilkår,
                tilsynsperioder);
    }

    private Map<LukketPeriode, Pleiebehov> toTilsynsbehov(PleiebehovResultat pleiebehov) {
        final Map<LukketPeriode, Pleiebehov> tilsynsbehov = new HashMap<>();
        pleiebehov.getPleieperioder().getPerioder().stream().forEach(p -> {
            tilsynsbehov.put(toLukketPeriode(p.getPeriode()), mapToPleiebehov(p.getGrad()));
        });
        return tilsynsbehov;
    }
    
    private LukketPeriode toLukketPeriode(DatoIntervallEntitet periode) {
        return new LukketPeriode(periode.getFomDato(), periode.getTomDato());
    }
    
    private Pleiebehov mapToPleiebehov(Pleiegrad grad) {
        switch (grad) {
        case INGEN: return Pleiebehov.PROSENT_0;
        case KONTINUERLIG_TILSYN: return Pleiebehov.PROSENT_100;
        case UTVIDET_KONTINUERLIG_TILSYN: return Pleiebehov.PROSENT_200;
        default: throw new IllegalStateException("Ukjent Pleiegrad: " + grad);
        }
    }

    private List<Arbeid> toArbeid(UttakAktivitet oppgittUttak) {
        // TODO: Skal vi ha arbeidsforhold på topp?
        final Map<InternArbeidsforholdRef, List<UttakAktivitetPeriode>> arbeidsforhold = new HashMap<>();
        oppgittUttak.getPerioder().forEach(p -> {
            List<UttakAktivitetPeriode> perioder = arbeidsforhold.get(p.getArbeidsforholdRef());
            if (perioder == null) {
                perioder = new ArrayList<>();
            }
            perioder.add(p);
            arbeidsforhold.put(p.getArbeidsforholdRef(), perioder);
        });
        
        return arbeidsforhold.entrySet().stream().map(e -> {
                var uttakAktivitetPeriode = e.getValue().get(0);
                final Map<LukketPeriode, ArbeidsforholdPeriodeInfo> perioder = new HashMap<>();
                e.getValue().forEach(p -> {
                    perioder.put(new LukketPeriode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()),
                            new ArbeidsforholdPeriodeInfo(p.getJobberNormaltPerUke(), Duration.ZERO)); // TODO: Sett riktig verdi.
                });
                
                return new Arbeid(
                        new Arbeidsforhold(
                                uttakAktivitetPeriode.getAktivitetType().getKode(),
                                uttakAktivitetPeriode.getArbeidsgiver().getArbeidsgiverOrgnr(),
                                uttakAktivitetPeriode.getArbeidsgiver().getArbeidsgiverAktørId().getId(),
                                uttakAktivitetPeriode.getArbeidsforholdRef().getReferanse()
                        ),
                        perioder
                );
            })
            .collect(Collectors.toList());
    }

    private HashMap<String, List<Vilkårsperiode>> toInngangsvilkår(Vilkårene vilkårene) {
        final HashMap<String, List<Vilkårsperiode>> inngangsvilkår = new HashMap<>();
        vilkårene.getVilkårene().forEach(v -> {
            final List<Vilkårsperiode> vilkårsperioder = v.getPerioder()
                    .stream()
                    .map(vp -> new Vilkårsperiode(new LukketPeriode(vp.getFom(), vp.getTom()), Utfall.valueOf(vp.getUtfall().getKode())))
                    .collect(Collectors.toList());
            inngangsvilkår.put(v.getVilkårType().getKode(), vilkårsperioder);
        });
        return inngangsvilkår;
    }
}
