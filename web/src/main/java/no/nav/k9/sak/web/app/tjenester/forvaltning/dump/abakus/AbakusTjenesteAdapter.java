package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.abakus;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerRequest;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.k9.sak.domene.abakus.AbakusTjeneste;
import no.nav.k9.sak.domene.abakus.K9AbakusTjeneste;
import no.nav.k9.sak.domene.abakus.mapping.MapInntektsmeldinger;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
@Default
@ScopedRestIntegration(scopeKey = "fpabakus.scope", defaultScope = "api://prod-fss.teamforeldrepenger.fpabakus/.default")
public class AbakusTjenesteAdapter {

    private AbakusTjeneste abakusTjeneste;

    private K9AbakusTjeneste k9AbakusTjeneste;

    private boolean k9AbakusEnabled;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    AbakusTjenesteAdapter() {
        // CDI proxy
    }

    @Inject
    public AbakusTjenesteAdapter(FagsakRepository fagsakRepository,
                                 BehandlingRepository behandlingRepository,
                                 SystemUserOidcRestClient restClient,
                                 @KonfigVerdi(value = "k9abakus.url") URI k9Abakusendpoint,
                                 @KonfigVerdi(value = "fpabakus.url") URI endpoint,
                                 @KonfigVerdi(value = "abakus.callback.url") URI callbackUrl,
                                 @KonfigVerdi(value = "k9.abakus.enabled", defaultVerdi = "false") boolean k9AbakusEnabled) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.abakusTjeneste = new AbakusTjeneste(restClient, endpoint, callbackUrl);
        this.k9AbakusTjeneste = new K9AbakusTjeneste(restClient, k9Abakusendpoint, callbackUrl);
        this.fagsakRepository = Objects.requireNonNull(fagsakRepository, "fagsakRepository");
        this.k9AbakusEnabled = k9AbakusEnabled;
    }

    public Optional<InntektArbeidYtelseGrunnlagDto> finnGrunnlag(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return Optional.ofNullable(this.hentGrunnlagHvisEksisterer(behandling));
    }

    public Set<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer) {
        Optional<Fagsak> fagsakOpt = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        if (fagsakOpt.isPresent()) {
            Fagsak fagsak = fagsakOpt.get();
            // Hent grunnlag fra abakus
            return hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer(), fagsak.getAktørId(), fagsak.getYtelseType());

        }
        return Set.of();
    }

    private InntektArbeidYtelseGrunnlagDto hentGrunnlagHvisEksisterer(Behandling behandling) {
        var request = initRequest(behandling);
        return hentGrunnlag(request);
    }

    private Set<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer, AktørId aktørId, FagsakYtelseType ytelseType) {
        Optional<Fagsak> fagsakOpt = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        if (fagsakOpt.isPresent()) {
            Fagsak fagsak = fagsakOpt.get();
            // Hent grunnlag fra abakus
            return hentOgMapAlleInntektsmeldinger(aktørId, fagsak.getSaksnummer(), ytelseType);

        }
        return Set.of();
    }

    private InntektsmeldingerDto hentUnikeInntektsmeldinger(InntektsmeldingerRequest request) {
        if (k9AbakusEnabled) {
            try {
                return k9AbakusTjeneste.hentUnikeUnntektsmeldinger(request);
            } catch (IOException e) {
                throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke hente inntektsmeldinger fra Abakus: " + e.getMessage(), e).toException();
            }
        } else {
            try {
                return abakusTjeneste.hentUnikeUnntektsmeldinger(request);
            } catch (IOException e) {
                throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke hente inntektsmeldinger fra Abakus: " + e.getMessage(), e).toException();
            }
        }
    }

    private InntektArbeidYtelseGrunnlagDto hentGrunnlag(InntektArbeidYtelseGrunnlagRequest request) {
        if (k9AbakusEnabled) {
            try {
                return abakusTjeneste.hentGrunnlag(request);
            } catch (IOException e) {
                throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke hente grunnlag fra Abakus: " + e.getMessage(), e).toException();
            }
        } else {
            try {
                return k9AbakusTjeneste.hentGrunnlag(request);
            } catch (IOException e) {
                throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke hente grunnlag fra Abakus: " + e.getMessage(), e).toException();
            }
        }
    }

    private InntektsmeldingAggregat mapResult(InntektsmeldingerDto dto) {
        var mapInntektsmeldinger = new MapInntektsmeldinger.MapFraDto();
        ArbeidsforholdInformasjonBuilder dummyBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        return mapInntektsmeldinger.map(dummyBuilder, dto);
    }

    private InntektArbeidYtelseGrunnlagRequest initRequest(Behandling behandling) {
        var request = new InntektArbeidYtelseGrunnlagRequest(new AktørIdPersonident(behandling.getAktørId().getId()));
        request.medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        request.medYtelseType(YtelseType.fraKode(behandling.getFagsakYtelseType().getKode()));
        request.forKobling(behandling.getUuid());
        request.medDataset(Arrays.asList(Dataset.values()));
        return request;
    }

    private InntektsmeldingerRequest initInntektsmeldingerRequest(AktørId aktørId, Saksnummer saksnummer, FagsakYtelseType ytelseType) {
        var request = new InntektsmeldingerRequest(new AktørIdPersonident(aktørId.getId()));
        request.setSaksnummer(saksnummer.getVerdi());
        request.setYtelseType(YtelseType.fraKode(ytelseType.getKode()));
        return request;
    }

    private LinkedHashSet<Inntektsmelding> hentOgMapAlleInntektsmeldinger(AktørId aktørId, Saksnummer saksnummer, FagsakYtelseType ytelseType) {
        var request = initInntektsmeldingerRequest(aktørId, saksnummer, ytelseType);
        var dto = hentUnikeInntektsmeldinger(request);
        var inntektsmeldinger = mapResult(dto).getAlleInntektsmeldinger();

        return inntektsmeldinger.stream()
            .sorted(Inntektsmelding.COMP_REKKEFØLGE)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
