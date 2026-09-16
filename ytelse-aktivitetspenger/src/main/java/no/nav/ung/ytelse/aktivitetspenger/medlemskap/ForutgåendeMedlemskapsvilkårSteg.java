package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.søknad.felles.type.Landkode;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class ForutgåendeMedlemskapsvilkårSteg extends VilkårVurderingSteg {

    private static final Logger log = LoggerFactory.getLogger(ForutgåendeMedlemskapsvilkårSteg.class);

    private VilkårResultatRepository vilkårResultatRepository;
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    public ForutgåendeMedlemskapsvilkårSteg() {
    }

    @Inject
    public ForutgåendeMedlemskapsvilkårSteg(VilkårResultatRepository vilkårResultatRepository,
                                            OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository,
                                            MottatteDokumentRepository mottatteDokumentRepository,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                            BehandlingRepository behandlingRepository,
                                            ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                                            VilkårTjeneste vilkårTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, perioderTilVurderingTjenester);
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
    }

    @Override
    public VilkårType getAktuellVilkårType() {
        return VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET;
    }

    @Override
    public Set<VilkårType> getVilkårAvhengigheter(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        EnumSet<VilkårType> avhengigheter = EnumSet.noneOf(VilkårType.class);
        avhengigheter.add(VilkårType.ALDERSVILKÅR);
        avhengigheter.add(VilkårType.SØKNADSFRIST);
        avhengigheter.add(VilkårType.BOSTEDSVILKÅR);
        avhengigheter.add(VilkårType.BISTANDSVILKÅR);
        avhengigheter.add(VilkårType.AKTIVITETSVILKÅR);
        avhengigheter.addAll(manuelleVilkårRekkefølgeTjeneste.finnManuelleVilkårSomErFør(getAktuellVilkårType(), ytelseType, behandlingType));
        return avhengigheter;
    }

    @Override
    public BehandleStegResultat utførResten(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var periodeTilVurdering = finnPerioderSomSkalVurderes(kontekst);
        if (periodeTilVurdering.isEmpty()) {
            log.info("Ingen perioder til vurdering");
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return vurderForutgåendeMedlemskap(periodeTilVurdering.segmenter(), behandlingId, kontekst.getFagsakId());
    }

    private BehandleStegResultat vurderForutgåendeMedlemskap(SequencedCollection<LocalDateSegment<Boolean>> perioderTilVurdering, Long behandlingId, Long fagsakId) {
        var grunnlagOpt = forutgåendeMedlemskapRepository.hentGrunnlagHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            log.info("Fant ingen grunnlag. Lager aksjonspunkt.");
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
        }

        var grunnlag = grunnlagOpt.get();
        var forutgåendeMedlemskapslandTidslinje = lagForutgåendeMedlemskapslandTidslinje(grunnlag, fagsakId);

        var stegerVurderinger = perioderTilVurdering.stream()
            .map(periode -> vurder(periode.getLocalDateInterval(), forutgåendeMedlemskapslandTidslinje))
            .toList();

        var trengerManuellVurdering = stegerVurderinger.stream()
            .anyMatch(v -> !v.vurdering().filterValue(u -> u != Utfall.OPPFYLT).isEmpty());

        if (trengerManuellVurdering) {
            log.info("Fant utenlandsopphold, lager aksjonspunkt for vilkårsvurdering.");
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
        }

        oppfyllVilkår(behandlingId, forutgåendeMedlemskapslandTidslinje, stegerVurderinger);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static StegVurdering vurder(LocalDateInterval periode, LocalDateTimeline<String> landTidslinje) {
        LocalDate virkningsdato = periode.getFomDato();
        var forutgåendePeriodeTilVurdering = new LocalDateInterval(virkningsdato.minusYears(5), virkningsdato.minusDays(1));
        var forutgåendeMedlemskapsvurdering = new LocalDateTimeline<>(forutgåendePeriodeTilVurdering, Boolean.TRUE)
            .combine(landTidslinje, ForutgåendeMedlemskapsvilkårSteg::vurderUtenlandsopphold, JoinStyle.LEFT_JOIN);
        return new StegVurdering(periode, forutgåendePeriodeTilVurdering, forutgåendeMedlemskapsvurdering);
    }

    private void oppfyllVilkår(Long behandlingId, LocalDateTimeline<String> utenlandsoppholdTidslinje, List<StegVurdering> stegVurderinger) {
        var jsonMapper = new VilkårJsonObjectMapper();
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårTjeneste.hentVilkårResultat(behandlingId));
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);

        stegVurderinger.forEach(stegVurdering -> {
            var regelInput = jsonMapper.writeValueAsString(new RegelInput(utenlandsoppholdTidslinje));
            var regelEvaluering = jsonMapper.writeValueAsString(stegVurdering);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(stegVurdering.periode().getFomDato(), stegVurdering.periode().getTomDato())
                .medUtfall(Utfall.OPPFYLT)
                .medAvslagsårsak(null)
                .medRegelInput(regelInput)
                .medRegelEvaluering(regelEvaluering));
        });

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());
    }

    private static LocalDateSegment<Utfall> vurderUtenlandsopphold(LocalDateInterval intervall, LocalDateSegment<Boolean> forutgåendePeriodeTilVurdering, LocalDateSegment<String> landkode) {
        if (landkode == null || landkode.getValue() == null) {
            return new LocalDateSegment<>(intervall, Utfall.IKKE_VURDERT);
        }
        if (landkode.getValue().equals("NOR")) {
            return new LocalDateSegment<>(intervall, Utfall.OPPFYLT);
        }
        return new LocalDateSegment<>(intervall, Utfall.IKKE_OPPFYLT);
    }

    private LocalDateTimeline<String> lagForutgåendeMedlemskapslandTidslinje(OppgittForutgåendeMedlemskapGrunnlag grunnlag, Long fagsakId) {
        var journalpostIder = grunnlag.getOppgittePerioder().stream()
            .map(OppgittForutgåendeMedlemskapPeriode::getJournalpostId)
            .toList();

        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsakId, journalpostIder);
        var nyesteJournalpostId = mottatteDokumenter.stream()
            .max(Comparator.comparing(MottattDokument::getMottattTidspunkt))
            .map(MottattDokument::getJournalpostId)
            .orElseThrow();

        var nyesteGrunnlagPeriode = grunnlag.getOppgittePerioder().stream()
            .filter(p -> p.getJournalpostId().equals(nyesteJournalpostId))
            .findFirst()
            .orElseThrow();

        var antattBostedNorgeTidslinje = new LocalDateTimeline<>(nyesteGrunnlagPeriode.getPeriode().getFomDato(), nyesteGrunnlagPeriode.getPeriode().getTomDato(), Landkode.NORGE.getLandkode());

        var utenlandsoppholdTidslinje = new LocalDateTimeline<>(
            nyesteGrunnlagPeriode.getUtenlandsopphold().stream()
                .map(b -> new LocalDateSegment<>(b.getPeriode().getFomDato(), b.getPeriode().getTomDato(), b.getLandkode()))
                .toList());

        return utenlandsoppholdTidslinje.crossJoin(antattBostedNorgeTidslinje);
    }

    record RegelInput(LocalDateTimeline<String> bostederLandkodeTidslinje) { }

    record StegVurdering(LocalDateInterval periode, LocalDateInterval forutgåendePeriode, LocalDateTimeline<Utfall> vurdering) {}

}
