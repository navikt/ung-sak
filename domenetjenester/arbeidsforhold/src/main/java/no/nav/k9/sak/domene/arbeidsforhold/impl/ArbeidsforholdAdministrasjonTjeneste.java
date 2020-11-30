package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static java.util.Collections.emptyList;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidType.AA_REGISTER_TYPER;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.NYTT_ARBEIDSFORHOLD;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde.INNTEKTSKOMPONENTEN;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.ArbeidsforholdWrapper;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyrtePerioder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdAksjonspunktÅrsak;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdIdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.MottattInntektsmeldingDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PermisjonDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.Stillingsprosent;

/**
 * Håndterer administrasjon(saksbehandlers input) vedrørende arbeidsforhold.
 */
@Dependent
public class ArbeidsforholdAdministrasjonTjeneste {

    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    ArbeidsforholdAdministrasjonTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdAdministrasjonTjeneste(VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste,
                                                ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                                InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    /**
     * Oppretter en builder for å lagre ned overstyringen av arbeidsforhold
     *
     * @param behandlingId behandlingen sin ID
     * @return buildern
     */
    public ArbeidsforholdInformasjonBuilder opprettBuilderFor(Long behandlingId) {
        return ArbeidsforholdInformasjonBuilder.oppdatere(inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId));
    }

    /**
     * Rydder opp i inntektsmeldinger som blir erstattet
     *
     * @param behandlingId behandlingId
     * @param aktørId      aktørId
     * @param builder      ArbeidsforholdsOverstyringene som skal lagrers
     */
    public void lagre(Long behandlingId, AktørId aktørId, ArbeidsforholdInformasjonBuilder builder) {
        inntektArbeidYtelseTjeneste.lagreArbeidsforhold(behandlingId, aktørId, builder);
    }

    /**
     * Avsjekk arbeidsforhold mot inntektsmeldinger.
     */
    public Set<ArbeidsforholdWrapper> hentArbeidsforholdFerdigUtledet(BehandlingReferanse ref,
                                                                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                      UtledArbeidsforholdParametere param) {
        AktørId aktørId = ref.getAktørId();
        // TODO: Jobb for å bli kvitt ..
        LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();

        var inntektsmeldinger = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));

        var overstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();

        var inntektsmeldingerForGrunnlag = iayGrunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(emptyList());

        var alleYrkesaktiviteter = filter.getYrkesaktiviteter();

        Set<ArbeidsforholdWrapper> arbeidsforhold = new LinkedHashSet<>(utledArbeidsforholdFraInntektsmeldinger(
            filter,
            inntektsmeldinger, alleYrkesaktiviteter, overstyringer, skjæringstidspunkt, iayGrunnlag.getArbeidsforholdInformasjon()));

        arbeidsforhold.addAll(utledArbeidsforholdFraYrkesaktivitet(
            filter, overstyringer, inntektsmeldinger, iayGrunnlag.getArbeidsforholdInformasjon(), skjæringstidspunkt));

        arbeidsforhold.addAll(utledArbeidsforholdFraArbeidsforholdInformasjon(filter,
            overstyringer, alleYrkesaktiviteter, inntektsmeldingerForGrunnlag, skjæringstidspunkt));

        sjekkHarAksjonspunktForVurderArbeidsforhold(ref, arbeidsforhold, param.getVurderArbeidsforhold());

        return arbeidsforhold;
    }

    public Set<InntektArbeidYtelseArbeidsforholdV2Dto> hentArbeidsforhold(BehandlingReferanse ref,
                                                                          InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                          UtledArbeidsforholdParametere param) {

        var inntektsmeldinger = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());

        var arbeidsforholdInformasjon = iayGrunnlag.getArbeidsforholdInformasjon();
        var filter = new YrkesaktivitetFilter(arbeidsforholdInformasjon, iayGrunnlag.getAktørArbeidFraRegister(ref.getAktørId()));

        var yrkesaktiviteter = filter.getAlleYrkesaktiviteter();

        var arbeidsforhold = new LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto>();
        utledArbeidsforholdFraInntektsmeldinger(arbeidsforhold, inntektsmeldinger, arbeidsforholdInformasjon);
        utledArbeidsforholdFraYrkesaktivteter(arbeidsforhold, yrkesaktiviteter, arbeidsforholdInformasjon);
        utledArbeidsforholdFraArbeidsforholdInformasjon(arbeidsforhold, filter.getArbeidsforholdOverstyringer(), arbeidsforholdInformasjon);

        if (param.getVurderArbeidsforhold()) {
            markerArbeidsforholdMedAksjonspunktÅrsaker(arbeidsforhold, ref, arbeidsforholdInformasjon);
        }

        return arbeidsforhold;
    }

    private void markerArbeidsforholdMedAksjonspunktÅrsaker(LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto> arbeidsforhold, BehandlingReferanse ref, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        if (!arbeidsforhold.isEmpty()) {
            var vurderinger = vurderArbeidsforholdTjeneste.vurderMedÅrsak(ref).entrySet().stream().filter(it -> it.getValue().stream().anyMatch(at -> !at.getÅrsaker().isEmpty())).collect(Collectors.toList());
            vurderinger.forEach(entry -> mapVurdering(arbeidsforhold, entry, arbeidsforholdInformasjon));
        }
    }

    private void mapVurdering(LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto> arbeidsforhold, Map.Entry<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> entry, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        entry.getValue().forEach(af -> {
            var dto = finnEllerOpprett(arbeidsforhold, entry.getKey(), af.getRef(), arbeidsforholdInformasjon);
            dto.setAksjonspunktÅrsaker(af.getÅrsaker().stream().map(it -> ArbeidsforholdAksjonspunktÅrsak.fraKode(it.name())).collect(Collectors.toSet()));
        });
    }

    private void utledArbeidsforholdFraArbeidsforholdInformasjon(LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto> result,
                                                                 Collection<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer,
                                                                 Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        arbeidsforholdOverstyringer.stream()
            .filter(it -> Set.of(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING, ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER).contains(it.getHandling()))
            .forEach(overstyring -> mapOverstyring(result, overstyring, arbeidsforholdInformasjon));
    }

    private void mapOverstyring(LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto> result,
                                ArbeidsforholdOverstyring overstyring,
                                Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var dto = finnEllerOpprett(result, overstyring.getArbeidsgiver(), overstyring.getArbeidsforholdRef(), arbeidsforholdInformasjon);
        dto.leggTilKilde(ArbeidsforholdKilde.SAKSBEHANDLER);
        dto.setHandlingType(overstyring.getHandling());
        dto.setStillingsprosent(overstyring.getStillingsprosent().getVerdi());
        dto.setAnsettelsesPerioder(mapAnsettelsesPerioder(overstyring.getArbeidsforholdOverstyrtePerioder()));
        dto.setBegrunnelse(overstyring.getBegrunnelse());
    }

    private Set<PeriodeDto> mapAnsettelsesPerioder(List<ArbeidsforholdOverstyrtePerioder> arbeidsforholdOverstyrtePerioder) {
        return arbeidsforholdOverstyrtePerioder.stream()
            .map(it -> new PeriodeDto(it.getOverstyrtePeriode().getFomDato(), it.getOverstyrtePeriode().getTomDato()))
            .collect(Collectors.toSet());
    }

    private void utledArbeidsforholdFraYrkesaktivteter(LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto> result,
                                                       Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                       Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {

        yrkesaktiviteter.forEach(yr -> mapYrkesaktivitet(result, yr, arbeidsforholdInformasjon));

    }

    private void mapYrkesaktivitet(LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto> result,
                                   Yrkesaktivitet yr,
                                   Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var dto = finnEllerOpprett(result, yr.getArbeidsgiver(), yr.getArbeidsforholdRef(), arbeidsforholdInformasjon);

        dto.leggTilKilde(ArbeidsforholdKilde.AAREGISTERET);
        dto.setAnsettelsesPerioder(mapAnsettelsesPerioder(yr.getAnsettelsesPeriode()));
        dto.setPermisjoner(mapPermisjoner(yr.getPermisjon()));
        dto.setStillingsprosent(yr.getStillingsprosentFor(LocalDate.now()).map(Stillingsprosent::getVerdi).orElse(BigDecimal.ZERO));
    }

    private InntektArbeidYtelseArbeidsforholdV2Dto finnEllerOpprett(LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto> result, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
        var collect = result.stream()
            .filter(it -> gjelderSammeArbeidsforhold(it, arbeidsgiver, arbeidsforholdRef))
            .collect(Collectors.toList());

        if (collect.isEmpty()) {
            var dto = new InntektArbeidYtelseArbeidsforholdV2Dto(arbeidsgiver, mapArbeidsforholdsId(arbeidsgiver, arbeidsforholdRef, arbeidsforholdInformasjon));
            result.add(dto);
            return dto;
        }
        if (collect.size() > 1) {
            throw new IllegalStateException("Flere arbeidsforhold med samme nøkkel, kan ikke forekomme.");
        }
        return collect.get(0);
    }

    private List<PermisjonDto> mapPermisjoner(Collection<Permisjon> permisjon) {
        return permisjon.stream()
            .map(it -> new PermisjonDto(it.getFraOgMed(), it.getTilOgMed(), it.getProsentsats().getVerdi(), it.getPermisjonsbeskrivelseType()))
            .collect(Collectors.toList());
    }

    private Set<PeriodeDto> mapAnsettelsesPerioder(Collection<AktivitetsAvtale> ansettelsesPeriode) {
        return ansettelsesPeriode.stream()
            .map(AktivitetsAvtale::getPeriode)
            .map(it -> new PeriodeDto(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toSet());
    }

    private void utledArbeidsforholdFraInntektsmeldinger(LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto> result,
                                                         Set<Inntektsmelding> inntektsmeldinger,
                                                         Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        inntektsmeldinger.forEach(im -> mapInntektsmeldingTilArbeidsforhold(result, im, arbeidsforholdInformasjon));
    }

    private void mapInntektsmeldingTilArbeidsforhold(LinkedHashSet<InntektArbeidYtelseArbeidsforholdV2Dto> result,
                                                     Inntektsmelding im,
                                                     Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var dto = finnEllerOpprett(result, im.getArbeidsgiver(), im.getArbeidsforholdRef(), arbeidsforholdInformasjon);

        dto.leggTilInntektsmelding(new MottattInntektsmeldingDto(im.getJournalpostId(), im.getInnsendingstidspunkt(), DokumentStatus.GYLDIG, null));
        dto.leggTilKilde(ArbeidsforholdKilde.INNTEKTSMELDING);
    }

    private ArbeidsforholdIdDto mapArbeidsforholdsId(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        return new ArbeidsforholdIdDto(arbeidsforholdRef.getUUIDReferanse(), arbeidsforholdInformasjon.map(it -> it.finnEkstern(arbeidsgiver, arbeidsforholdRef)).map(EksternArbeidsforholdRef::getReferanse).orElse(null));
    }

    private boolean gjelderSammeArbeidsforhold(InntektArbeidYtelseArbeidsforholdV2Dto it, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return it.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator()) &&
            arbeidsforholdRef.equals(InternArbeidsforholdRef.ref(it.getArbeidsforhold().getInternArbeidsforholdId()));
    }

    private void sjekkHarAksjonspunktForVurderArbeidsforhold(BehandlingReferanse ref, Set<ArbeidsforholdWrapper> arbeidsforhold,
                                                             boolean vurderArbeidsforhold) {
        if (vurderArbeidsforhold && !arbeidsforhold.isEmpty()) {
            final Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> vurder = vurderArbeidsforholdTjeneste.vurderMedÅrsak(ref);
            for (ArbeidsforholdWrapper arbeidsforholdWrapper : arbeidsforhold) {
                for (Map.Entry<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverSetEntry : vurder.entrySet()) {
                    if (erAksjonspunktPå(arbeidsforholdWrapper, arbeidsgiverSetEntry)) {
                        arbeidsforholdWrapper.setHarAksjonspunkt(true);
                        arbeidsforholdWrapper.setBrukArbeidsforholdet(null);
                        arbeidsforholdWrapper.setFortsettBehandlingUtenInntektsmelding(null);
                    }
                }
            }
        }
    }

    private boolean erAksjonspunktPå(ArbeidsforholdWrapper arbeidsforholdWrapper, Map.Entry<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> entry) {
        if (arbeidsforholdWrapper.getKilde() == INNTEKTSKOMPONENTEN) {
            return entry.getKey().getIdentifikator().equals(arbeidsforholdWrapper.getArbeidsgiverIdentifikator());
        }

        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.ref(arbeidsforholdWrapper.getArbeidsforholdId());
        return entry.getKey().getIdentifikator().equals(arbeidsforholdWrapper.getArbeidsgiverIdentifikator())
            && entry.getValue().stream().map(ArbeidsforholdMedÅrsak::getRef).anyMatch(arbeidsforholdRef::gjelderFor);
    }

    private List<ArbeidsforholdWrapper> utledArbeidsforholdFraInntektsmeldinger(YrkesaktivitetFilter filter, Set<Inntektsmelding> inntektsmeldinger,
                                                                                Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                                                List<ArbeidsforholdOverstyring> overstyringer,
                                                                                LocalDate skjæringstidspunkt,
                                                                                Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        return inntektsmeldinger.stream()
            .map(i -> mapInntektsmeldingTilWrapper(filter, alleYrkesaktiviteter, overstyringer, skjæringstidspunkt, i,
                arbeidsforholdInformasjon))
            .collect(Collectors.toList());
    }

    private ArbeidsforholdWrapper mapInntektsmeldingTilWrapper(YrkesaktivitetFilter filter,
                                                               Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                               List<ArbeidsforholdOverstyring> overstyringer,
                                                               LocalDate skjæringstidspunkt,
                                                               Inntektsmelding inntektsmelding,
                                                               Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        ArbeidsforholdWrapper wrapper = new ArbeidsforholdWrapper();
        mapArbeidsgiver(wrapper, inntektsmelding.getArbeidsgiver(), overstyringer);
        wrapper.setMottattDatoInntektsmelding(inntektsmelding.getMottattDato());

        InternArbeidsforholdRef arbeidsforholdRef = inntektsmelding.getArbeidsforholdRef();
        if (arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            wrapper.setArbeidsforholdId(arbeidsforholdRef.getReferanse());
        }
        List<Yrkesaktivitet> yrkesaktiviteter = finnYrkesAktiviteter(alleYrkesaktiviteter, inntektsmelding.getArbeidsgiver(), arbeidsforholdRef);

        wrapper.setPermisjoner(UtledPermisjonSomFørerTilAksjonspunkt.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        Optional<ArbeidsforholdOverstyring> overstyring = finnMatchendeOverstyring(inntektsmelding, overstyringer);

        if (overstyring.isPresent()) {
            ArbeidsforholdOverstyring os = overstyring.get();
            wrapper.setBrukPermisjon(UtledBrukAvPermisjonForWrapper.utled(os.getBekreftetPermisjon()));
            wrapper.setBegrunnelse(os.getBegrunnelse());
            wrapper.setStillingsprosent(os.getStillingsprosent() != null ? os.getStillingsprosent().getVerdi()
                : UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
            Optional<DatoIntervallEntitet> ansettelsesperiode = os.getArbeidsforholdOverstyrtePerioder().stream().findFirst()
                .map(ArbeidsforholdOverstyrtePerioder::getOverstyrtePeriode);
            wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
            wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
            wrapper.setLagtTilAvSaksbehandler(os.getHandling().equals(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER));
            wrapper.setBasertPåInntektsmelding(os.getHandling().equals(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING));
        } else {
            Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, skjæringstidspunkt, false);
            wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
            wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
            wrapper.setStillingsprosent(UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        }
        // setter disse
        wrapper.setBrukArbeidsforholdet(true);
        final Arbeidsgiver arbeidsgiver = inntektsmelding.getArbeidsgiver();
        final Boolean erNyttArbeidsforhold = erNyttArbeidsforhold(overstyringer, arbeidsgiver, inntektsmelding.getArbeidsforholdRef());
        wrapper.setErNyttArbeidsforhold(erNyttArbeidsforhold);
        wrapper.setFortsettBehandlingUtenInntektsmelding(false);
        wrapper.setIkkeRegistrertIAaRegister(yrkesaktiviteter.isEmpty());
        wrapper.setVurderOmSkalErstattes(false);
        wrapper.setHarErsattetEttEllerFlere(!inntektsmelding.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold());
        wrapper.setErEndret(sjekkOmFinnesIOverstyr(overstyringer, inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef()));
        wrapper.setSkjaeringstidspunkt(skjæringstidspunkt);
        wrapper.setKilde(yrkesaktiviteter.stream().anyMatch(ya -> !filter.getAnsettelsesPerioder(ya).isEmpty()) ? ArbeidsforholdKilde.AAREGISTERET
            : ArbeidsforholdKilde.INNTEKTSMELDING);

        if (arbeidsforholdInformasjon.isPresent()) {
            var eksternArbeidsforholdRef = arbeidsforholdInformasjon.get().finnEkstern(inntektsmelding.getArbeidsgiver(), arbeidsforholdRef);
            wrapper.setEksternArbeidsforholdId(eksternArbeidsforholdRef.getReferanse());
        }
        return wrapper;
    }

    private boolean sjekkOmFinnesIOverstyr(List<ArbeidsforholdOverstyring> overstyringer, Arbeidsgiver arbeidsgiver,
                                           InternArbeidsforholdRef arbeidsforholdRef) {
        return overstyringer.stream()
            .anyMatch(overstyring -> overstyring.getArbeidsgiver().equals(arbeidsgiver)
                && Objects.equals(overstyring.getArbeidsforholdRef(), arbeidsforholdRef)
                && overstyring.erOverstyrt());
    }

    private boolean erNyttArbeidsforhold(List<ArbeidsforholdOverstyring> overstyringer, Arbeidsgiver arbeidsgiver,
                                         InternArbeidsforholdRef arbeidsforholdRef) {
        return overstyringer.stream().anyMatch(ov -> ov.getHandling().equals(NYTT_ARBEIDSFORHOLD) && ov.getArbeidsgiver().equals(arbeidsgiver)
            && ov.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef));
    }

    private List<Yrkesaktivitet> finnYrkesAktiviteter(Collection<Yrkesaktivitet> yrkesaktiviteter, Arbeidsgiver arbeidsgiver,
                                                      InternArbeidsforholdRef arbeidsforholdRef) {
        return yrkesaktiviteter.stream()
            .filter(yr -> yr.gjelderFor(arbeidsgiver, arbeidsforholdRef))
            .collect(Collectors.toList());
    }

    private List<ArbeidsforholdWrapper> utledArbeidsforholdFraArbeidsforholdInformasjon(YrkesaktivitetFilter filter,
                                                                                        List<ArbeidsforholdOverstyring> overstyringer,
                                                                                        Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                                                        List<Inntektsmelding> alleInntektsmeldinger,
                                                                                        LocalDate skjæringstidspunkt) {
        return overstyringer.stream()
            .filter(ArbeidsforholdOverstyring::erOverstyrt)
            .map(a -> mapOverstyringTilWrapper(filter, a, alleYrkesaktiviteter, alleInntektsmeldinger, skjæringstidspunkt))
            .collect(Collectors.toList());
    }

    private ArbeidsforholdWrapper mapOverstyringTilWrapper(YrkesaktivitetFilter filter,
                                                           ArbeidsforholdOverstyring overstyring,
                                                           Collection<Yrkesaktivitet> alleYrkesaktiviteter,
                                                           List<Inntektsmelding> alleInntektsmeldinger,
                                                           LocalDate skjæringstidspunkt) {
        final Arbeidsgiver arbeidsgiver = overstyring.getArbeidsgiver();
        final InternArbeidsforholdRef arbeidsforholdRef = overstyring.getArbeidsforholdRef();
        final List<Yrkesaktivitet> yrkesaktiviteter = finnYrkesAktiviteter(alleYrkesaktiviteter, arbeidsgiver, arbeidsforholdRef);
        final LocalDate mottattDatoInntektsmelding = mottattInntektsmelding(overstyring, alleInntektsmeldinger);
        ArbeidsforholdWrapper wrapper = new ArbeidsforholdWrapper();
        if (!yrkesaktiviteter.isEmpty()) {
            Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, skjæringstidspunkt, false);
            wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
            wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
            wrapper.setKilde(utledKilde(ansettelsesperiode, mottattDatoInntektsmelding, overstyring.getHandling()));
            if (!overstyring.getArbeidsforholdOverstyrtePerioder().isEmpty()) {
                Optional<DatoIntervallEntitet> overstyrtAnsettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, skjæringstidspunkt, true);
                wrapper.setOverstyrtTom(overstyrtAnsettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
            }
            wrapper.setIkkeRegistrertIAaRegister(false);
            wrapper.setHandlingType(overstyring.getHandling());
            wrapper.setStillingsprosent(UtledStillingsprosent.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        } else {
            wrapper.setKilde(utledKilde(Optional.empty(), mottattDatoInntektsmelding, overstyring.getHandling()));
            wrapper.setIkkeRegistrertIAaRegister(true);
            List<ArbeidsforholdOverstyrtePerioder> arbeidsforholdOverstyrtePerioder = overstyring.getArbeidsforholdOverstyrtePerioder();
            if (arbeidsforholdOverstyrtePerioder.size() != 1) {
                throw new IllegalStateException("Forventer kun ett innslag i listen");
            }
            wrapper.setFomDato(arbeidsforholdOverstyrtePerioder.get(0).getOverstyrtePeriode().getFomDato());
            wrapper.setTomDato(arbeidsforholdOverstyrtePerioder.get(0).getOverstyrtePeriode().getTomDato());
            wrapper.setStillingsprosent(overstyring.getStillingsprosent().getVerdi());
        }
        mapArbeidsgiverForOverstyring(wrapper, arbeidsgiver, List.of(overstyring));
        mapArbeidsforholdHandling(wrapper, overstyring);
        wrapper.setArbeidsforholdId(arbeidsforholdRef.getReferanse());
        wrapper.setBegrunnelse(overstyring.getBegrunnelse());
        wrapper.setMottattDatoInntektsmelding(mottattDatoInntektsmelding);
        wrapper.setErEndret(true);

        wrapper.setSkjaeringstidspunkt(skjæringstidspunkt);
        wrapper.setBrukMedJustertPeriode(Objects.equals(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE, overstyring.getHandling()));
        wrapper.setPermisjoner(UtledPermisjonSomFørerTilAksjonspunkt.utled(filter, yrkesaktiviteter, skjæringstidspunkt));
        wrapper.setBrukPermisjon(UtledBrukAvPermisjonForWrapper.utled(overstyring.getBekreftetPermisjon()));
        return wrapper;
    }

    private void mapArbeidsforholdHandling(ArbeidsforholdWrapper wrapper, ArbeidsforholdOverstyring overstyring) {
        if (overstyring.getHandling().equals(ArbeidsforholdHandlingType.IKKE_BRUK)) {
            wrapper.setBrukArbeidsforholdet(false);
            wrapper.setFortsettBehandlingUtenInntektsmelding(false);
        } else if (overstyring.getHandling().equals(SLÅTT_SAMMEN_MED_ANNET)) {
            wrapper.setErSlettet(true);
        } else if (overstyring.getHandling().equals(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)) {
            wrapper.setLagtTilAvSaksbehandler(true);
            wrapper.setBrukArbeidsforholdet(true);
            wrapper.setFortsettBehandlingUtenInntektsmelding(true);
        } else if (overstyring.getHandling().equals(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING)) {
            wrapper.setBasertPåInntektsmelding(true);
            wrapper.setBrukArbeidsforholdet(true);
        } else if (overstyring.getHandling().equals(ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG)) {
            wrapper.setInntektMedTilBeregningsgrunnlag(true);
            wrapper.setBasertPåInntektsmelding(true);
            wrapper.setBrukArbeidsforholdet(true);
        } else {
            wrapper.setFortsettBehandlingUtenInntektsmelding(true);
            wrapper.setBrukArbeidsforholdet(true);
        }
    }


    private ArbeidsforholdKilde utledKilde(Optional<DatoIntervallEntitet> avtale, LocalDate mottattDatoInntektsmelding, ArbeidsforholdHandlingType handling) {
        if (Objects.equals(handling, ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)) {
            return ArbeidsforholdKilde.SAKSBEHANDLER;
        }
        if (avtale.isPresent()) {
            return ArbeidsforholdKilde.AAREGISTERET;
        }
        if (mottattDatoInntektsmelding != null) {
            return ArbeidsforholdKilde.INNTEKTSMELDING;
        }
        return INNTEKTSKOMPONENTEN;
    }

    private LocalDate mottattInntektsmelding(ArbeidsforholdOverstyring overstyringEntitet, List<Inntektsmelding> alleInntektsmeldinger) {
        final Optional<LocalDate> mottattDato = alleInntektsmeldinger
            .stream()
            .filter(im -> overstyringEntitet.getArbeidsgiver().equals(im.getArbeidsgiver())
                && overstyringEntitet.getArbeidsforholdRef().gjelderFor(im.getArbeidsforholdRef()))
            .findFirst()
            .map(Inntektsmelding::getMottattDato);

        return mottattDato.orElse(null);
    }

    private List<ArbeidsforholdWrapper> utledArbeidsforholdFraYrkesaktivitet(YrkesaktivitetFilter filter,
                                                                             List<ArbeidsforholdOverstyring> overstyringer,
                                                                             Set<Inntektsmelding> inntektsmeldinger,
                                                                             Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon,
                                                                             LocalDate skjæringstidspunkt) {
        DatoIntervallEntitet stp = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt);
        return filter.getYrkesaktiviteter().stream()
            .filter(yr -> AA_REGISTER_TYPER.contains(yr.getArbeidType()))
            .filter(yr -> harIkkeFåttInntektsmelding(yr, inntektsmeldinger))
            .filter(yr -> filter.getAnsettelsesPerioder(yr).stream().map(AktivitetsAvtale::getPeriode).anyMatch(periode -> periode.overlapper(stp)) ||
                filter.getAnsettelsesPerioder(yr).stream().map(AktivitetsAvtale::getPeriode)
                    .anyMatch(periode -> periode.getFomDato().isAfter(skjæringstidspunkt)))
            .filter(yr -> filtreVekkLagtTilAvSaksbehandler(yr, overstyringer))
            .map(yr -> mapYrkesaktivitetAAREG(filter, yr, overstyringer, arbeidsforholdInformasjon, skjæringstidspunkt))
            .collect(Collectors.toList());
    }

    private boolean filtreVekkLagtTilAvSaksbehandler(Yrkesaktivitet yrkesaktivitet, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream().noneMatch(o -> yrkesaktivitet.gjelderFor(o.getArbeidsgiver(), o.getArbeidsforholdRef())
            && o.getHandling().equals(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER));
    }

    private ArbeidsforholdWrapper mapYrkesaktivitetAAREG(YrkesaktivitetFilter filter,
                                                         Yrkesaktivitet yrkesaktivitet,
                                                         List<ArbeidsforholdOverstyring> overstyringer,
                                                         Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon, LocalDate skjæringstidspunkt) {
        final Optional<ArbeidsforholdOverstyring> arbeidsforholdOverstyringEntitet = finnMatchendeOverstyring(yrkesaktivitet, overstyringer);
        final Arbeidsgiver arbeidsgiver = yrkesaktivitet.getArbeidsgiver();
        final InternArbeidsforholdRef arbeidsforholdRef = yrkesaktivitet.getArbeidsforholdRef();
        final Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktivitet, skjæringstidspunkt, false);
        ArbeidsforholdWrapper wrapper = new ArbeidsforholdWrapper();
        wrapper.setStillingsprosent(UtledStillingsprosent.utled(filter, yrkesaktivitet, skjæringstidspunkt));
        wrapper.setFomDato(ansettelsesperiode.map(DatoIntervallEntitet::getFomDato).orElse(null));
        wrapper.setTomDato(ansettelsesperiode.map(DatoIntervallEntitet::getTomDato).orElse(null));
        wrapper.setArbeidsforholdId(arbeidsforholdRef.getReferanse());
        if (arbeidsforholdInformasjon.isPresent()) {
            var eksternArbeidsforholdRef = arbeidsforholdInformasjon.get().finnEkstern(arbeidsgiver, arbeidsforholdRef);
            wrapper.setEksternArbeidsforholdId(eksternArbeidsforholdRef.getReferanse());
        }
        wrapper.setKilde(ArbeidsforholdKilde.AAREGISTERET);
        wrapper.setIkkeRegistrertIAaRegister(false);
        wrapper.setBrukArbeidsforholdet(true);
        wrapper.setFortsettBehandlingUtenInntektsmelding(harTattStillingTil(yrkesaktivitet, overstyringer));
        wrapper.setErEndret(sjekkOmFinnesIOverstyr(overstyringer, arbeidsgiver, yrkesaktivitet.getArbeidsforholdRef()));
        wrapper.setSkjaeringstidspunkt(skjæringstidspunkt);
        wrapper.setPermisjoner(UtledPermisjonSomFørerTilAksjonspunkt.utled(filter, List.of(yrkesaktivitet), skjæringstidspunkt));
        mapArbeidsgiver(wrapper, arbeidsgiver, overstyringer);
        arbeidsforholdOverstyringEntitet.ifPresent(ov -> {
            wrapper.setHandlingType(ov.getHandling());
            wrapper.setBegrunnelse(ov.getBegrunnelse());
            wrapper.setBrukPermisjon(UtledBrukAvPermisjonForWrapper.utled(ov.getBekreftetPermisjon()));
            wrapper
                .setInntektMedTilBeregningsgrunnlag(Objects.equals(ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG, ov.getHandling()) ? Boolean.FALSE : null);
            wrapper.setBrukMedJustertPeriode(Objects.equals(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE, ov.getHandling()));
            if (!ov.getArbeidsforholdOverstyrtePerioder().isEmpty()) {
                LocalDate overstyrtTom = UtledAnsettelsesperiode.utled(filter, yrkesaktivitet, skjæringstidspunkt, true)
                    .map(DatoIntervallEntitet::getTomDato).orElse(null);
                wrapper.setOverstyrtTom(overstyrtTom);
            }
        });
        return wrapper;
    }

    private void mapArbeidsgiver(ArbeidsforholdWrapper wrapper, Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        Optional<String> navnOpt = overstyringer.stream()
            .filter(o -> o.getArbeidsgiver().equals(arbeidsgiver) && o.getArbeidsgiverNavn() != null)
            .map(ArbeidsforholdOverstyring::getArbeidsgiverNavn)
            .findAny();
        if (opplysninger != null) {
            wrapper.setNavn(opplysninger.getNavn());
            navnOpt.ifPresent(wrapper::setNavn);
            if (arbeidsgiver.erAktørId()) {
                wrapper.setPersonArbeidsgiverIdentifikator(opplysninger.getIdentifikator());
            }
        } else {
            wrapper.setNavn("N/A");
        }
        wrapper.setArbeidsgiverIdentifikator(arbeidsgiver.getIdentifikator());
    }

    private void mapArbeidsgiverForOverstyring(ArbeidsforholdWrapper wrapper, Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        if (opplysninger == null) {
            wrapper.setNavn("N/A");
            wrapper.setArbeidsgiverIdentifikator(arbeidsgiver.getIdentifikator());
        } else {
            if (OrgNummer.erKunstig(opplysninger.getIdentifikator())) {
                Optional<String> navnOpt = overstyringer.stream()
                    .filter(o -> o.getArbeidsgiver().equals(arbeidsgiver) && o.getArbeidsgiverNavn() != null)
                    .map(ArbeidsforholdOverstyring::getArbeidsgiverNavn)
                    .findAny();
                navnOpt.ifPresent(wrapper::setNavn);
            } else {
                wrapper.setNavn(opplysninger.getNavn());
            }
            if (arbeidsgiver.erAktørId()) {
                wrapper.setPersonArbeidsgiverIdentifikator(opplysninger.getIdentifikator());
            }
            wrapper.setArbeidsgiverIdentifikator(arbeidsgiver.getIdentifikator());
        }
    }

    private boolean harTattStillingTil(Yrkesaktivitet yr, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
            .anyMatch(ov -> ov.kreverIkkeInntektsmelding()
                && yr.gjelderFor(ov.getArbeidsgiver(), ov.getArbeidsforholdRef()));
    }

    private boolean harIkkeFåttInntektsmelding(Yrkesaktivitet yr, Set<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream().noneMatch(i -> yr.gjelderFor(i.getArbeidsgiver(), i.getArbeidsforholdRef()));
    }

    private Optional<ArbeidsforholdOverstyring> finnMatchendeOverstyring(Yrkesaktivitet ya, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
            .filter(os -> ya.gjelderFor(os.getArbeidsgiver(), os.getArbeidsforholdRef()))
            .filter(ArbeidsforholdOverstyring::erOverstyrt)
            .findFirst();
    }

    private Optional<ArbeidsforholdOverstyring> finnMatchendeOverstyring(Inntektsmelding inntektsmelding, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream()
            .filter(os -> Objects.equals(inntektsmelding.getArbeidsgiver(), os.getArbeidsgiver()) &&
                inntektsmelding.getArbeidsforholdRef().gjelderFor(os.getArbeidsforholdRef()))
            .filter(ArbeidsforholdOverstyring::erOverstyrt)
            .findFirst();
    }

    /**
     * Param klasse for å kunne ta inn parametere som styrer utleding av arbeidsforhold.
     */
    public static class UtledArbeidsforholdParametere {
        private final boolean vurderArbeidsforhold;

        public UtledArbeidsforholdParametere(boolean vurderArbeidsforhold) {
            this.vurderArbeidsforhold = vurderArbeidsforhold;
        }

        public boolean getVurderArbeidsforhold() {
            return vurderArbeidsforhold;
        }
    }

}
