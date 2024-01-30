package no.nav.k9.sak.inngangsvilkår.medlemskap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.k9.sak.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.inngangsvilkår.medlemskap.regelmodell.Medlemskapsvilkår;
import no.nav.k9.sak.inngangsvilkår.medlemskap.regelmodell.MedlemskapsvilkårGrunnlag;
import no.nav.k9.sak.inngangsvilkår.medlemskap.regelmodell.PersonStatusType;
import no.nav.k9.sak.typer.Arbeidsgiver;

@ApplicationScoped
public class VurderLøpendeMedlemskap {

    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private MedlemskapRepository medlemskapRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private MedlemskapPerioderTjeneste medTjeneste;
    private UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerMedlemskap;

    VurderLøpendeMedlemskap() {
        //CDI
    }

    @Inject
    public VurderLøpendeMedlemskap(BasisPersonopplysningTjeneste personopplysningTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   MedlemskapRepository medlemskapRepository,
                                   MedlemskapPerioderTjeneste medTjeneste,
                                   UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerMedlemskapTjeneste,
                                   InntektArbeidYtelseTjeneste iayTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.medlemskapRepository = medlemskapRepository;
        this.medTjeneste = medTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.utledVurderingsdatoerMedlemskap = utledVurderingsdatoerMedlemskapTjeneste;
    }

    //TODO hvorfor ligger denne her fortsatt (ubrukt) og hvorfor ligger det masse tester under /resources???
    @Deprecated
    public Map<LocalDate, VilkårData> vurderMedlemskap(Long behandlingId) {
        Map<LocalDate, VilkårData> resultat = new TreeMap<>();

        for (Map.Entry<LocalDate, MedlemskapsvilkårGrunnlag> entry : lagGrunnlag(behandlingId).entrySet()) {
            VilkårData data = evaluerGrunnlag(entry.getValue(), DatoIntervallEntitet.fraOgMed(entry.getKey()));
            if (data.getUtfallType().equals(Utfall.OPPFYLT)) {
                resultat.put(entry.getKey(), data);
            } else if (data.getUtfallType().equals(Utfall.IKKE_OPPFYLT)) {
                if (data.getVilkårUtfallMerknad() == null) {
                    throw new IllegalStateException("Forventer at vilkår utfall merknad er satt når vilkåret blir satt til IKKE_OPPFYLT for grunnlag:" + entry.getValue().toString());
                }
                resultat.put(entry.getKey(), data);
                break;
            }
        }
        return resultat;
    }

    private VilkårData evaluerGrunnlag(MedlemskapsvilkårGrunnlag grunnlag, DatoIntervallEntitet periode) {
        Evaluation evaluation = new Medlemskapsvilkår().evaluer(grunnlag);
        return new VilkårUtfallOversetter().oversett(VilkårType.MEDLEMSKAPSVILKÅRET, evaluation, grunnlag, periode);
    }

    private Map<LocalDate, MedlemskapsvilkårGrunnlag> lagGrunnlag(Long behandlingId) {
        return lagGrunnlagMedForlengelsesPerioder(behandlingId).getGrunnlagPerVurderingsdato();
    }

    private GrunnlagOgPerioder lagGrunnlagMedForlengelsesPerioder(Long behandlingId) {
        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        Optional<VurdertMedlemskapPeriodeEntitet> vurdertMedlemskapPeriode = medlemskap.flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap);
        var vurderingsdatoerMedForlengelse = utledVurderingsdatoerMedlemskap.finnVurderingsdatoerMedForlengelse(behandlingId);

        if (vurderingsdatoerMedForlengelse.getDatoerTilVurdering().isEmpty()) {
            return new GrunnlagOgPerioder(vurderingsdatoerMedForlengelse.getPerioderTilVurdering(), Collections.emptyMap(), vurderingsdatoerMedForlengelse.getForlengelser());
        }

        Map<LocalDate, VurdertLøpendeMedlemskapEntitet> map = mapVurderingFraSaksbehandler(vurdertMedlemskapPeriode);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        Map<LocalDate, MedlemskapsvilkårGrunnlag> resulatat = new TreeMap<>();
        for (LocalDate vurderingsdato : vurderingsdatoerMedForlengelse.getDatoerTilVurdering()) {
            Optional<VurdertLøpendeMedlemskapEntitet> vurdertOpt = Optional.ofNullable(map.get(vurderingsdato));
            PersonopplysningerAggregat aggregatOptional = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(ref.getBehandlingId(), ref.getAktørId(), vurderingsdato);
            MedlemskapsvilkårGrunnlag grunnlag = new MedlemskapsvilkårGrunnlag(
                brukerErMedlemEllerIkkeRelevantPeriode(medlemskap, vurdertOpt, aggregatOptional, vurderingsdato), // FP VK 2.13
                tilPersonStatusType(aggregatOptional),
                brukerNorskNordisk(aggregatOptional),
                vurdertOpt.map(VurdertLøpendeMedlemskapEntitet::getErEøsBorger).orElse(utledBasertPåStatsborgerskap(aggregatOptional))
            );

            grunnlag.setHarSøkerArbeidsforholdOgInntekt(finnOmSøkerHarArbeidsforholdOgInntekt(behandling, vurderingsdato));
            grunnlag.setBrukerAvklartLovligOppholdINorge(vurdertOpt.map(VurdertLøpendeMedlemskapEntitet::getLovligOppholdVurdering).orElse(true));
            grunnlag.setBrukerAvklartBosatt(vurdertOpt.map(VurdertLøpendeMedlemskapEntitet::getBosattVurdering).orElse(true));
            grunnlag.setBrukerAvklartOppholdsrett(vurdertOpt.map(VurdertLøpendeMedlemskapEntitet::getOppholdsrettVurdering).orElse(true));
            // FP VK 2.2 Er bruker avklart som pliktig eller frivillig medlem?
            grunnlag.setBrukerAvklartPliktigEllerFrivillig(erAvklartSomPliktigEllerFrivillingMedlem(vurdertOpt, medlemskap, vurderingsdato));

            resulatat.put(vurderingsdato, grunnlag);
        }
        return new GrunnlagOgPerioder(vurderingsdatoerMedForlengelse.getPerioderTilVurdering(), resulatat, vurderingsdatoerMedForlengelse.getForlengelser());
    }

    private Boolean utledBasertPåStatsborgerskap(PersonopplysningerAggregat aggregat) {
        final Optional<StatsborgerskapEntitet> statsborgerskap = aggregat.getStatsborgerskapFor(aggregat.getSøker().getAktørId()).stream().findFirst();
        final var region = statsborgerskap.map(StatsborgerskapEntitet::getRegion).orElse(null);
        return Region.NORDEN.equals(region) || Region.EOS.equals(region);
    }

    private Map<LocalDate, VurdertLøpendeMedlemskapEntitet> mapVurderingFraSaksbehandler(Optional<VurdertMedlemskapPeriodeEntitet> vurdertMedlemskapPeriode) {
        Map<LocalDate, VurdertLøpendeMedlemskapEntitet> vurderingFraSaksbehandler = new HashMap<>();
        vurdertMedlemskapPeriode.ifPresent(v -> {
            Set<VurdertLøpendeMedlemskapEntitet> perioder = v.getPerioder();
            for (VurdertLøpendeMedlemskapEntitet vurdertLøpendeMedlemskap : perioder) {
                vurderingFraSaksbehandler.put(vurdertLøpendeMedlemskap.getVurderingsdato(), vurdertLøpendeMedlemskap);
            }
        });
        return vurderingFraSaksbehandler;
    }

    private boolean brukerErMedlemEllerIkkeRelevantPeriode(Optional<MedlemskapAggregat> medlemskap, Optional<VurdertLøpendeMedlemskapEntitet> vurdertMedlemskap,
                                                           PersonopplysningerAggregat søker, LocalDate vurderingsdato) {
        if (vurdertMedlemskap.isPresent()
            && MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.get().getMedlemsperiodeManuellVurdering())) {
            return true;
        }

        Set<MedlemskapPerioderEntitet> medlemskapPerioder = medlemskap.isPresent() ? medlemskap.get().getRegistrertMedlemskapPerioder()
            : Collections.emptySet();
        boolean erAvklartMaskineltSomIkkeMedlem = medTjeneste.brukerMaskineltAvklartSomIkkeMedlem(søker,
            medlemskapPerioder, vurderingsdato);
        boolean erAvklartManueltSomIkkeMedlem = erAvklartSomIkkeMedlem(vurdertMedlemskap);

        return !(erAvklartMaskineltSomIkkeMedlem || erAvklartManueltSomIkkeMedlem);
    }

    private boolean erAvklartSomIkkeMedlem(Optional<VurdertLøpendeMedlemskapEntitet> medlemskap) {
        return medlemskap.isPresent() && medlemskap.get().getMedlemsperiodeManuellVurdering() != null
            && MedlemskapManuellVurderingType.UNNTAK.equals(medlemskap.get().getMedlemsperiodeManuellVurdering());
    }

    private boolean erAvklartSomPliktigEllerFrivillingMedlem(Optional<VurdertLøpendeMedlemskapEntitet> vurdertLøpendeMedlemskap,
                                                             Optional<MedlemskapAggregat> medlemskap, LocalDate vurderingsdato) {
        if (vurdertLøpendeMedlemskap.isPresent()) {
            VurdertMedlemskap vurdertMedlemskap = vurdertLøpendeMedlemskap.get();
            if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                MedlemskapManuellVurderingType.MEDLEM.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                return true;
            }
            if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                return false;
            }
        }
        return medTjeneste.brukerMaskineltAvklartSomFrivilligEllerPliktigMedlem(
            medlemskap.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet()), vurderingsdato);
    }

    private boolean brukerNorskNordisk(PersonopplysningerAggregat aggregat) {
        final Optional<StatsborgerskapEntitet> statsborgerskap = aggregat.getStatsborgerskapFor(aggregat.getSøker().getAktørId())
            .stream()
            .findFirst();
        return Region.NORDEN.equals(statsborgerskap.map(StatsborgerskapEntitet::getRegion).orElse(null));
    }

    private PersonStatusType tilPersonStatusType(PersonopplysningerAggregat aggregat) {
        PersonstatusType type = Optional.ofNullable(aggregat.getPersonstatusFor(aggregat.getSøker().getAktørId())).map(PersonstatusEntitet::getPersonstatus).orElse(null);

        if (PersonstatusType.BOSA.equals(type) || PersonstatusType.ADNR.equals(type)) {
            return PersonStatusType.BOSA;
        } else if (PersonstatusType.UTVA.equals(type)) {
            return PersonStatusType.UTVA;
        } else if (PersonstatusType.erDød(type)) {
            return PersonStatusType.DØD;
        }
        return null;
    }

    private boolean finnOmSøkerHarArbeidsforholdOgInntekt(Behandling behandling, LocalDate vurderingsdato) {
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlagOptional = iayTjeneste.finnGrunnlag(behandling.getId());

        if (inntektArbeidYtelseGrunnlagOptional.isPresent()) {
            InntektArbeidYtelseGrunnlag grunnlag = inntektArbeidYtelseGrunnlagOptional.get();
            var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandling.getAktørId())).før(vurderingsdato);

            if (filter.getYrkesaktiviteter().isEmpty()) {
                return false;
            }

            List<Arbeidsgiver> arbeidsgivere = finnRelevanteArbeidsgivereMedLøpendeAvtaleEllerAvtaleSomErGyldigPåStp(vurderingsdato, filter);
            if (arbeidsgivere.isEmpty()) {
                return false;
            }

            var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(behandling.getAktørId())).før(vurderingsdato);

            return inntektFilter.filterPensjonsgivende().getAlleInntekter().stream()
                .anyMatch(e -> arbeidsgivere.contains(e.getArbeidsgiver()));
        }
        return false;
    }

    private List<Arbeidsgiver> finnRelevanteArbeidsgivereMedLøpendeAvtaleEllerAvtaleSomErGyldigPåStp(LocalDate skjæringstidspunkt, YrkesaktivitetFilter filter) {
        List<Arbeidsgiver> relevanteArbeid = new ArrayList<>();
        for (Yrkesaktivitet yrkesaktivitet : filter.getYrkesaktiviteter()) {
            if (yrkesaktivitet.erArbeidsforhold()) {
                // Hvis har en løpende avtale fom før skjæringstidspunktet eller den som dekker skjæringstidspunktet
                boolean harLøpendeAvtaleFørSkjæringstidspunkt = filter.getAnsettelsesPerioder(yrkesaktivitet)
                    .stream()
                    .anyMatch(aktivitetsAvtale -> harLøpendeArbeidsforholdFørSkjæringstidspunkt(skjæringstidspunkt, aktivitetsAvtale));
                if (harLøpendeAvtaleFørSkjæringstidspunkt) {
                    relevanteArbeid.add(yrkesaktivitet.getArbeidsgiver());
                }
            }
        }
        return relevanteArbeid;
    }

    private boolean harLøpendeArbeidsforholdFørSkjæringstidspunkt(LocalDate skjæringstidspunkt, AktivitetsAvtale aktivitetsAvtale) {
        LocalDate fomDato = aktivitetsAvtale.getPeriode().getFomDato();
        LocalDate tomDato = aktivitetsAvtale.getPeriode().getTomDato();
        return (aktivitetsAvtale.getErLøpende() && fomDato.isBefore(skjæringstidspunkt))
            || (fomDato.isBefore(skjæringstidspunkt) && tomDato.isAfter(skjæringstidspunkt));
    }

    public VurdertMedlemskapOgForlengelser vurderMedlemskapOgHåndterForlengelse(Long behandlingId) {

        var grunnlagOgPerioder = lagGrunnlagMedForlengelsesPerioder(behandlingId);

        return vurderPerioderMedForlengelse(grunnlagOgPerioder);
    }

    /**
     * Vurderer medlemskap per vurderingsdato
     * Dersom en vurdering er ikke oppfylt skal vi ikke lagre flere vurderinger for den perioden (i tilfelle vi har flere vurderingsdatoer innenfor samme periode)
     * Vi må lagre vurderinger for andre perioder selv om en av periodene får ikke godkjent (i tilfelle vi har flere skjæringstidspunkt)
     */
    VurdertMedlemskapOgForlengelser vurderPerioderMedForlengelse(GrunnlagOgPerioder grunnlagOgPerioder) {
        Map<LocalDate, VilkårData> resultat = new TreeMap<>();
        Set<DatoIntervallEntitet> perioderIkkeOppfylt = new HashSet<>();

        for (Map.Entry<LocalDate, MedlemskapsvilkårGrunnlag> entry : grunnlagOgPerioder.getGrunnlagPerVurderingsdato().entrySet()) {
            final DatoIntervallEntitet perioden = finnPeriodenSomOverlapper(entry.getKey(), grunnlagOgPerioder);
            if (perioderIkkeOppfylt.contains(perioden)) {
                continue;
            }

            final LocalDate tilOgMedDato = utledTilOgMedDato(entry.getKey(), grunnlagOgPerioder.getGrunnlagPerVurderingsdato().keySet(), perioden);
            final VilkårData data = evaluerGrunnlag(entry.getValue(), DatoIntervallEntitet.fraOgMedTilOgMed(entry.getKey(), tilOgMedDato));

            if (data.getUtfallType().equals(Utfall.OPPFYLT)) {
                resultat.put(entry.getKey(), data);
            } else if (data.getUtfallType().equals(Utfall.IKKE_OPPFYLT)) {
                if (data.getVilkårUtfallMerknad() == null) {
                    throw new IllegalStateException("Forventer at vilkår utfall merknad er satt når vilkåret blir satt til IKKE_OPPFYLT for grunnlag:" + entry.getValue().toString());
                }
                resultat.put(entry.getKey(), data);
                perioderIkkeOppfylt.add(perioden);
            }
        }
        return new VurdertMedlemskapOgForlengelser(resultat, grunnlagOgPerioder.getForlengelsesPerioder());
    }

    private LocalDate utledTilOgMedDato(LocalDate key, Set<LocalDate> vurderingsdatoer, DatoIntervallEntitet perioden) {
        Set<LocalDate> vurderingsdatoerUtenOmNøkkel = vurderingsdatoer.stream().filter(it -> !Objects.equals(key, it)).collect(Collectors.toCollection(TreeSet::new));
        return vurderingsdatoerUtenOmNøkkel
            .stream()
            .filter(perioden::inkluderer)
            .filter(it -> it.isAfter(key))
            .min(LocalDate::compareTo)
            .map(it -> it.minusDays(1))
            .orElse(perioden.getTomDato());
    }

    private DatoIntervallEntitet finnPeriodenSomOverlapper(LocalDate key, GrunnlagOgPerioder grunnlagOgPerioder) {
        var perioder = new TreeSet<>(grunnlagOgPerioder.getPerioderTilVurdering());
        perioder.addAll(grunnlagOgPerioder.getForlengelsesPerioder());

        var perioderSomOverlapper = perioder.stream().filter(it -> it.inkluderer(key)).collect(Collectors.toSet());
        if (perioderSomOverlapper.size() != 1) {
            throw new IllegalStateException("Vurderer dato ikke tilknyttet periode");
        }
        return perioderSomOverlapper.iterator().next();
    }
}
