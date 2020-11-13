package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.k9.sak.domene.iay.modell.BekreftetPermisjon;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforhold;
import no.nav.k9.sak.kontrakt.arbeidsforhold.AvklarArbeidsforholdDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.Stillingsprosent;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarArbeidsforhold.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarArbeidsforholdOppdaterer implements AksjonspunktOppdaterer<AvklarArbeidsforhold> {

    private static final String FIKTIVT_ORG = OrgNummer.KUNSTIG_ORG;
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
            return OppdateringResultat.utenTransisjon().build();
        }
        var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingId);

        var arbeidsforholdLagtTilAvSaksbehandler = arbeidsforhold.stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getLagtTilAvSaksbehandler()))
            .collect(Collectors.toList());
        // Arbeidsforhold basert på saksbehandler lagt til
        if (!arbeidsforholdLagtTilAvSaksbehandler.isEmpty()) {
            lagHistorikkinnslag(arbeidsforholdLagtTilAvSaksbehandler);
            leggTilArbeidsforhold(informasjonBuilder, arbeidsforholdLagtTilAvSaksbehandler);
        }

        var arbeidsforholdBasertPåInntektsmelding = arbeidsforhold.stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getBasertPaInntektsmelding()))
            .collect(Collectors.toList());
        // Arbeidsforhold basert på inntektsmelding
        if (!arbeidsforholdBasertPåInntektsmelding.isEmpty()) {
            lagHistorikkinnslag(arbeidsforholdBasertPåInntektsmelding);
            leggTilArbeidsforhold(informasjonBuilder, arbeidsforholdBasertPåInntektsmelding);
        }

        // Andre overstyringer av opprinnelige arbeidsforhold
        // TODO: ER BARE BEGRUNNELSE OG PERMISJON IGJEN SOM SETTES HER? (MULIG OGSÅ BRUK/IKKE_BRUK?)
        var opprinneligeArbeidsforhold = arbeidsforhold.stream()
            .filter(dto -> !Boolean.TRUE.equals(dto.getLagtTilAvSaksbehandler()) && !Boolean.TRUE.equals(dto.getBasertPaInntektsmelding()))
            .collect(Collectors.toList());
        if (!opprinneligeArbeidsforhold.isEmpty()) {
            var overstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId()).getArbeidsforholdOverstyringer();
            for (var dto : opprinneligeArbeidsforhold) {
                var ref = InternArbeidsforholdRef.ref(dto.getArbeidsforholdId());
                arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(param, dto, hentArbeidsgiver(dto), ref, overstyringer);
            }
            leggPåOverstyringPåOpprinnligeArbeidsforhold(informasjonBuilder, opprinneligeArbeidsforhold);
        }

        if (!arbeidsforholdLagtTilAvSaksbehandler.isEmpty() || !arbeidsforholdBasertPåInntektsmelding.isEmpty()) {
            håndterManuelleArbeidsforhold(param);
        }
        // krever totrinn hvis saksbehandler har tatt stilling til dette aksjonspunktet
        arbeidsforholdTjeneste.lagre(param.getBehandlingId(), param.getAktørId(), informasjonBuilder);
        return OppdateringResultat.utenTransisjon().medTotrinn().build();

    }

    private void lagHistorikkinnslag(List<AvklarArbeidsforholdDto> arbeidsforholdListe) {
        // lag historikkinnslag
        for (var arbeidsforholdDto : arbeidsforholdListe) {
            arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(arbeidsforholdDto, arbeidsforholdDto.getNavn(), Optional.empty());
        }
    }

    private void leggTilArbeidsforhold(ArbeidsforholdInformasjonBuilder informasjonBuilder,
                                       List<AvklarArbeidsforholdDto> arbeidsforholdListe) {
        for (var arbeidsforhold : arbeidsforholdListe) {
            ArbeidsforholdHandlingType handlingType = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforhold);
            Arbeidsgiver arbeidsgiver = hentArbeidsgiver(arbeidsforhold);
            ArbeidsforholdOverstyringBuilder overstyrt = leggTilOverstyrt(informasjonBuilder, arbeidsforhold, handlingType, arbeidsgiver);
            informasjonBuilder.leggTil(overstyrt);
        }
    }

    private ArbeidsforholdOverstyringBuilder leggTilOverstyrt(ArbeidsforholdInformasjonBuilder informasjonBuilder,
                                                              AvklarArbeidsforholdDto arbeidsforholdDto,
                                                              ArbeidsforholdHandlingType handlingType,
                                                              Arbeidsgiver arbeidsgiver) {

        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver,
            InternArbeidsforholdRef.ref(arbeidsforholdDto.getArbeidsforholdId()));
        var stillingsprosent = Objects.requireNonNull(arbeidsforholdDto.getStillingsprosent(), "stillingsprosent");
        return overstyringBuilder.medHandling(handlingType)
            .medAngittArbeidsgiverNavn(arbeidsforholdDto.getNavn())
            .medBeskrivelse(arbeidsforholdDto.getBegrunnelse())
            .medAngittStillingsprosent(new Stillingsprosent(stillingsprosent))
            .leggTilOverstyrtPeriode(arbeidsforholdDto.getFomDato(),
                arbeidsforholdDto.getTomDato() == null ? TIDENES_ENDE : arbeidsforholdDto.getTomDato());
    }

    private void leggPåOverstyringPåOpprinnligeArbeidsforhold(ArbeidsforholdInformasjonBuilder informasjonBuilder,
                                                              List<AvklarArbeidsforholdDto> arbeidsforhold) {

        // FIXME: Antar resten av koden her kan også ryke Marius? Fjerning av SLÅTT_SAMMEN_MED_ANNET gjør at mye her har røket allerede. Gjenstår
        // permisjon, begrunnelse
        for (AvklarArbeidsforholdDto arbeidsforholdDto : filtrerUtArbeidsforholdSomHarBlittErsattet(arbeidsforhold)) {

            ArbeidsforholdHandlingType handling = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);
            Arbeidsgiver arbeidsgiver = hentArbeidsgiver(arbeidsforholdDto);
            InternArbeidsforholdRef ref = InternArbeidsforholdRef.ref(arbeidsforholdDto.getArbeidsforholdId());

            ArbeidsforholdOverstyringBuilder overstyringBuilderFor = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                .medBeskrivelse(arbeidsforholdDto.getBegrunnelse())
                .medHandling(handling);

            if (arbeidsforholdDto.getBrukPermisjon() != null) {
                BekreftetPermisjon bekreftetPermisjon = UtledBekreftetPermisjon.utled(arbeidsforholdDto);
                overstyringBuilderFor.medBekreftetPermisjon(bekreftetPermisjon);
            }

            informasjonBuilder.leggTil(overstyringBuilderFor);
        }
    }

    private List<AvklarArbeidsforholdDto> filtrerUtArbeidsforholdSomHarBlittErsattet(List<AvklarArbeidsforholdDto> arbeidsforhold) {
        Set<String> filtrertUt = arbeidsforhold.stream()
            .map(AvklarArbeidsforholdDto::getErstatterArbeidsforholdId)
            .collect(Collectors.toSet());
        return arbeidsforhold.stream()
            .filter(a -> !filtrertUt.contains(a.getId()))
            .collect(Collectors.toList());
    }

    private Arbeidsgiver hentArbeidsgiver(AvklarArbeidsforholdDto dto) {
        Arbeidsgiver arbeidsgiver = dto.getLagtTilAvSaksbehandler()
            ? Arbeidsgiver.virksomhet(FIKTIVT_ORG)
            : (OrgNummer.erGyldigOrgnr(dto.getArbeidsgiverIdentifikator())
                ? Arbeidsgiver.virksomhet(dto.getArbeidsgiverIdentifikator())
                : Arbeidsgiver.person(new AktørId(dto.getArbeidsgiverIdentifikator())));
        return arbeidsgiver;

    }

    /**
     * @deprecated Denne blir lett misbrukt, siden man antagelig ønsker å gjøre mer enn kun fjerne saksbehandlet versjon. Bruk derfor heller
     *             {@link #lagreIayAggregat(Long, InntektArbeidYtelseAggregatBuilder)} etter du er ferdig med alle endringer du trenger å gjøre
     */
    @Deprecated(forRemoval = true)
    private void håndterManuelleArbeidsforhold(AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();
        inntektArbeidYtelseTjeneste.fjernSaksbehandletVersjon(behandlingId);
    }
}
