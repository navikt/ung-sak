package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.k9.sak.domene.arbeidsforhold.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_SØKER;
import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.domene.arbeidsforhold.ArbeidsforholdWrapper;
import no.nav.k9.sak.domene.arbeidsforhold.BehandlingRelaterteYtelserMapper;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.YtelserKonsolidertTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.Gradering;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.UtsettelsePeriode;
import no.nav.k9.sak.kontrakt.arbeidsforhold.GraderingPeriodeDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforhold;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektsmeldingDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PermisjonDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.RelaterteYtelserDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.TilgrensendeYtelserDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.UtsettelsePeriodeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Periode;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class InntektArbeidYtelseDtoMapper {

    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private YtelserKonsolidertTjeneste ytelseTjeneste;

    public InntektArbeidYtelseDtoMapper() {
        // for CDI proxy
    }

    @Inject
    public InntektArbeidYtelseDtoMapper(ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                        YtelserKonsolidertTjeneste ytelseTjeneste,
                                        InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                        VirksomhetTjeneste virksomhetTjeneste) {

        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.ytelseTjeneste = ytelseTjeneste;
    }

    public InntektArbeidYtelseDto mapFra(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, SakInntektsmeldinger sakInntektsmeldinger,
                                         UtledArbeidsforholdParametere param) {
        InntektArbeidYtelseDto dto = new InntektArbeidYtelseDto();
        mapRelaterteYtelser(dto, ref, iayGrunnlag);

        mapArbeidsforhold(dto, ref, param, iayGrunnlag, sakInntektsmeldinger);
        dto.setInntektsmeldinger(lagInntektsmeldingDto(ref, iayGrunnlag));
        return dto;
    }

    private void mapArbeidsforhold(InntektArbeidYtelseDto dto, BehandlingReferanse ref, UtledArbeidsforholdParametere param,
                                   InntektArbeidYtelseGrunnlag iayGrunnlag, SakInntektsmeldinger sakInntektsmeldinger) {
        Set<ArbeidsforholdWrapper> arbeidsforholdSet = arbeidsforholdAdministrasjonTjeneste.hentArbeidsforholdFerdigUtledet(ref, iayGrunnlag,
            sakInntektsmeldinger, param);
        dto.setSkalKunneLeggeTilNyeArbeidsforhold(skalKunneLeggeTilNyeArbeidsforhold(arbeidsforholdSet));
        dto.setSkalKunneLageArbeidsforholdBasrtPåInntektsmelding(skalKunneLageArbeidsforholdFraInntektsmelding(arbeidsforholdSet));
        dto.setArbeidsforhold(arbeidsforholdSet.stream().map(this::mapArbeidsforhold).collect(Collectors.toList()));
    }

    private boolean skalKunneLageArbeidsforholdFraInntektsmelding(Set<ArbeidsforholdWrapper> arbeidsforholdSet) {
        return !arbeidsforholdSet.isEmpty() && arbeidsforholdSet.stream().allMatch(a -> a.getKilde().equals(ArbeidsforholdKilde.INNTEKTSMELDING));
    }

    private InntektArbeidYtelseArbeidsforhold mapArbeidsforhold(ArbeidsforholdWrapper wrapper) {
        InntektArbeidYtelseArbeidsforhold dto = new InntektArbeidYtelseArbeidsforhold();
        dto.setId(lagId(wrapper));
        dto.setFomDato(wrapper.getFomDato());
        dto.setTomDato(wrapper.getTomDato() != null && wrapper.getTomDato().equals(Tid.TIDENES_ENDE) ? null : wrapper.getTomDato());
        dto.setNavn(wrapper.getNavn());
        mapArbeidsgiverIdentifikator(wrapper, dto);
        dto.setBrukArbeidsforholdet(wrapper.getBrukArbeidsforholdet());
        if (wrapper.getBrukArbeidsforholdet() != null && wrapper.getBrukArbeidsforholdet()) {
            dto.setErNyttArbeidsforhold(wrapper.getErNyttArbeidsforhold());
            dto.setFortsettBehandlingUtenInntektsmelding(wrapper.getFortsettBehandlingUtenInntektsmelding());
        }
        dto.setArbeidsforholdId(wrapper.getArbeidsforholdId());
        dto.setEksternArbeidsforholdId(wrapper.getEksternArbeidsforholdId());
        dto.setIkkeRegistrertIAaRegister(wrapper.getIkkeRegistrertIAaRegister());
        dto.setHarErstattetEttEllerFlere(wrapper.getHarErsattetEttEllerFlere());
        dto.setErstatterArbeidsforholdId(wrapper.getErstatterArbeidsforhold());
        dto.setKilde(wrapper.getKilde());
        dto.setMottattDatoInntektsmelding(wrapper.getMottattDatoInntektsmelding());
        dto.setTilVurdering(wrapper.isHarAksjonspunkt());
        dto.setBegrunnelse(wrapper.getBegrunnelse());
        dto.setVurderOmSkalErstattes(wrapper.getVurderOmSkalErstattes());
        dto.setStillingsprosent(wrapper.getStillingsprosent());
        dto.setErSlettet(wrapper.getErSlettet());
        dto.setErEndret(wrapper.getErEndret());
        dto.setHandlingType(wrapper.getHandlingType());
        dto.setBrukMedJustertPeriode(wrapper.getBrukMedJustertPeriode());
        dto.setLagtTilAvSaksbehandler(wrapper.getLagtTilAvSaksbehandler());
        dto.setBasertPaInntektsmelding(wrapper.getBasertPåInntektsmelding());
        dto.setInntektMedTilBeregningsgrunnlag(wrapper.getInntektMedTilBeregningsgrunnlag());
        dto.setSkjaeringstidspunkt(wrapper.getSkjaeringstidspunkt());
        dto.setBrukPermisjon(wrapper.getBrukPermisjon());
        dto.setPermisjoner(wrapper.getPermisjoner().stream().map(this::byggPermisjonDto).collect(Collectors.toList()));
        dto.setOverstyrtTom(Tid.TIDENES_ENDE.equals(wrapper.getOverstyrtTom()) ? null : wrapper.getOverstyrtTom());
        return dto;
    }

    private boolean skalKunneLeggeTilNyeArbeidsforhold(Set<ArbeidsforholdWrapper> arbeidsforholdSet) {
        if (arbeidsforholdSet.isEmpty()) {
            return true;
        }
        return arbeidsforholdSet.stream().anyMatch(wrapper -> Objects.equals(wrapper.getHandlingType(), ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER));
    }

    private void mapArbeidsgiverIdentifikator(ArbeidsforholdWrapper wrapper, InntektArbeidYtelseArbeidsforhold dto) {
        dto.setArbeidsgiverIdentifikator(wrapper.getArbeidsgiverIdentifikator());
        if (gjelderVirksomhet(wrapper)) {
            dto.setArbeidsgiverIdentifikatorGUI(wrapper.getArbeidsgiverIdentifikator());
        } else {
            dto.setArbeidsgiverIdentifikatorGUI(wrapper.getPersonArbeidsgiverIdentifikator());
        }
    }

    private boolean gjelderVirksomhet(ArbeidsforholdWrapper wrapper) {
        return wrapper.getPersonArbeidsgiverIdentifikator() == null;
    }

    private String lagId(ArbeidsforholdWrapper wrapper) {
        return wrapper.getArbeidsgiverIdentifikator() + "-" + wrapper.getArbeidsforholdId();
    }

    private List<InntektsmeldingDto> lagInntektsmeldingDto(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        LocalDate dato = ref.getUtledetSkjæringstidspunkt();
        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref.getAktørId(), dato, iayGrunnlag);
        return inntektsmeldinger.stream()
            .map(inntektsmelding -> {
                Optional<Virksomhet> virksomhet = virksomhetTjeneste.hentVirksomhet(inntektsmelding.getArbeidsgiver().getOrgnr());
                return mapInntektsmelding(inntektsmelding, virksomhet);
            })
            .collect(Collectors.toList());
    }

    private InntektsmeldingDto mapInntektsmelding(Inntektsmelding inntektsmelding, Optional<Virksomhet> virksomhet) {
        var dto = new InntektsmeldingDto();
        Arbeidsgiver arb = inntektsmelding.getArbeidsgiver();
        dto.setArbeidsgiver(arb.getErVirksomhet()
            ? virksomhet.orElseThrow(() -> {
                return new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + arb.getOrgnr());
            }).getNavn()
            : "Privatperson"); // TODO skal navn på privatperson som arbeidsgiver hentes fra et register?

        dto.setArbeidsgiverOrgnr(arb.getIdentifikator());
        dto.setArbeidsgiverStartdato(inntektsmelding.getStartDatoPermisjon().orElse(null));
        dto.setInnsendingstidspunkt(inntektsmelding.getInnsendingstidspunkt());

        List<UtsettelsePeriode> utsettelser = inntektsmelding.getUtsettelsePerioder();
        if (utsettelser != null) {
            dto.setUtsettelsePerioder(utsettelser
                .stream()
                .map(p -> new UtsettelsePeriodeDto(new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()), p.getÅrsak()))
                .collect(Collectors.toList()));
        }

        List<Gradering> graderinger = inntektsmelding.getGraderinger();
        if (graderinger != null) {
            dto.setGraderingPerioder(graderinger
                .stream()
                .map(p -> new GraderingPeriodeDto(new Periode(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()), p.getArbeidstidProsent().getVerdi()))
                .collect(Collectors.toList()));
        }

        dto.setGetRefusjonBeløpPerMnd(inntektsmelding.getRefusjonBeløpPerMnd());
        
        return dto;
    }

    private void mapRelaterteYtelser(InntektArbeidYtelseDto dto, BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag) {
        dto.setRelatertTilgrensendeYtelserForSoker(mapTilDtoSøker(hentRelaterteYtelser(grunnlag, ref.getAktørId())));
    }

    private List<RelaterteYtelserDto> mapTilDtoSøker(List<TilgrensendeYtelserDto> tilgrensendeYtelserDtos) {
        return BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(tilgrensendeYtelserDtos, RELATERT_YTELSE_TYPER_FOR_SØKER);
    }

    private List<TilgrensendeYtelserDto> hentRelaterteYtelser(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId) {
        final List<TilgrensendeYtelserDto> relatertYtelser = new ArrayList<>();
        // Relaterte yteleser fra InntektArbeidYtelseAggregatet
        relatertYtelser.addAll(mapYtelseForAktørTilTilgrensedeYtelser(grunnlag, aktørId));

        return relatertYtelser;
    }

    private List<TilgrensendeYtelserDto> mapYtelseForAktørTilTilgrensedeYtelser(InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, AktørId aktørId) {
        return ytelseTjeneste.utledYtelserRelatertTilBehandling(aktørId, inntektArbeidYtelseGrunnlag, Optional.empty());
    }

    private PermisjonDto byggPermisjonDto(Permisjon permisjon) {
        return new PermisjonDto(
            permisjon.getFraOgMed(),
            permisjon.getTilOgMed() == null || TIDENES_ENDE.equals(permisjon.getTilOgMed()) ? null : permisjon.getTilOgMed(),
            permisjon.getProsentsats().getVerdi(),
            permisjon.getPermisjonsbeskrivelseType());
    }
}
