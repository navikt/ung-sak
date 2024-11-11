package no.nav.ung.sak.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.k9.felles.konfigurasjon.konfig.Tid.TIDENES_ENDE;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.ung.sak.kontrakt.arbeidsforhold.ArbeidsforholdAksjonspunktÅrsak;
import no.nav.ung.sak.kontrakt.arbeidsforhold.AvklarArbeidsforhold;
import no.nav.ung.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import no.nav.ung.sak.typer.Stillingsprosent;
import no.nav.ung.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarArbeidsforhold.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarArbeidsforholdOppdaterer implements AksjonspunktOppdaterer<AvklarArbeidsforhold> {

    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdHistorikkinnslagTjeneste arbeidsforholdHistorikkinnslagTjeneste;

    AvklarArbeidsforholdOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarArbeidsforholdOppdaterer(ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste,
                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                          ArbeidsforholdHistorikkinnslagTjeneste arbeidsforholdHistorikkinnslagTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsforholdHistorikkinnslagTjeneste = arbeidsforholdHistorikkinnslagTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarArbeidsforhold avklarArbeidsforholdDto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();

        List<AvklarArbeidsforholdDto> arbeidsforhold = avklarArbeidsforholdDto.getArbeidsforhold();
        if (arbeidsforhold.isEmpty()) {
            return OppdateringResultat.builder().build();
        }
        var grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingId);
        var arbeidsforholdInfo = arbeidsforholdTjeneste.hentArbeidsforhold(param.getRef(), grunnlag, new ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere(true))
            .stream()
            .filter(it -> harAksjonspunktForInntektsmeldingUtenArbeidsforhold(it) || harVærtHåndtertVedAksjonspunktFør(it))
            .collect(Collectors.toSet());

        var dtoerMedAvklaring = arbeidsforhold.stream()
            .filter(it -> arbeidsforholdInfo.stream()
                .anyMatch(at -> at.getId().equals(it.getId())))
            .collect(Collectors.toList());

        dtoerMedAvklaring.forEach(dto -> {
            lagHistorikkinnslag(dto);
            leggTilArbeidsforhold(informasjonBuilder, dto);
        });

        // krever totrinn hvis saksbehandler har tatt stilling til dette aksjonspunktet
        arbeidsforholdTjeneste.lagre(param.getBehandlingId(), param.getAktørId(), informasjonBuilder);

        return OppdateringResultat.builder().build();
    }

    private boolean harVærtHåndtertVedAksjonspunktFør(InntektArbeidYtelseArbeidsforholdV2Dto it) {
        return it.getKilde().contains(ArbeidsforholdKilde.SAKSBEHANDLER);
    }

    private boolean harAksjonspunktForInntektsmeldingUtenArbeidsforhold(InntektArbeidYtelseArbeidsforholdV2Dto it) {
        return it.getAksjonspunktÅrsaker().contains(ArbeidsforholdAksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD);
    }

    private void lagHistorikkinnslag(AvklarArbeidsforholdDto arbeidsforholdDto) {
        // lag historikkinnslag
        arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(arbeidsforholdDto, arbeidsforholdDto.getNavn());
    }

    private void leggTilArbeidsforhold(ArbeidsforholdInformasjonBuilder informasjonBuilder,
                                       AvklarArbeidsforholdDto arbeidsforhold) {
        Arbeidsgiver arbeidsgiver = arbeidsforhold.getArbeidsgiver();
        ArbeidsforholdOverstyringBuilder overstyrt = leggTilOverstyrt(informasjonBuilder, arbeidsforhold, arbeidsgiver);
        informasjonBuilder.leggTil(overstyrt);
    }

    private ArbeidsforholdOverstyringBuilder leggTilOverstyrt(ArbeidsforholdInformasjonBuilder informasjonBuilder,
                                                              AvklarArbeidsforholdDto arbeidsforholdDto,
                                                              Arbeidsgiver arbeidsgiver) {
        var handlingType = arbeidsforholdDto.getHandlingType();
        if (erIkkeGyldig(handlingType)) {
            throw new IllegalArgumentException("Ugyldig handling: " + handlingType);
        }
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver,
            InternArbeidsforholdRef.ref(arbeidsforholdDto.getArbeidsforhold().getInternArbeidsforholdId()));
        var stillingsprosent = Objects.requireNonNull(arbeidsforholdDto.getStillingsprosent(), "stillingsprosent");

        overstyringBuilder.medHandling(handlingType)
            .medBeskrivelse(arbeidsforholdDto.getBegrunnelse());

        if (List.of(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING, ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER).contains(handlingType)) {
            overstyringBuilder.medAngittArbeidsgiverNavn(arbeidsforholdDto.getNavn())
                .medAngittStillingsprosent(new Stillingsprosent(stillingsprosent));
            arbeidsforholdDto.getAnsettelsesPerioder()
                .forEach(periode -> overstyringBuilder.leggTilOverstyrtPeriode(periode.getFom(), periode.getTom() == null ? TIDENES_ENDE : periode.getTom()));
        }
        return overstyringBuilder;
    }

    private boolean erIkkeGyldig(ArbeidsforholdHandlingType handlingType) {
        return !EnumSet.of(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING,
            ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER,
            ArbeidsforholdHandlingType.BRUK).contains(handlingType);
    }
}
