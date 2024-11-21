package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.abakus;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.ung.sak.domene.abakus.AbakusTjeneste;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Dependent
@Default
@ScopedRestIntegration(scopeKey = "k9abakus.scope", defaultScope = "api://prod-fss.k9saksbehandling.k9-abakus/.default")
public class AbakusTjenesteAdapter {

    private AbakusTjeneste abakusTjeneste;
    private BehandlingRepository behandlingRepository;

    AbakusTjenesteAdapter() {
        // CDI proxy
    }

    @Inject
    public AbakusTjenesteAdapter(FagsakRepository fagsakRepository,
                                 BehandlingRepository behandlingRepository,
                                 SystemUserOidcRestClient restClient,
                                 @KonfigVerdi(value = "k9abakus.url") URI endpoint,
                                 @KonfigVerdi(value = "abakus.callback.url") URI callbackUrl) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.abakusTjeneste = new AbakusTjeneste(restClient, endpoint, callbackUrl);
    }

    public Optional<InntektArbeidYtelseGrunnlagDto> finnGrunnlag(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return Optional.ofNullable(this.hentGrunnlagHvisEksisterer(behandling));
    }

    private InntektArbeidYtelseGrunnlagDto hentGrunnlagHvisEksisterer(Behandling behandling) {
        var request = initRequest(behandling);
        return hentGrunnlag(request);
    }

    private InntektArbeidYtelseGrunnlagDto hentGrunnlag(InntektArbeidYtelseGrunnlagRequest request) {
        try {
            return abakusTjeneste.hentGrunnlag(request);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Kunne ikke hente grunnlag fra Abakus: " + e.getMessage(), e).toException();
        }
    }

    private InntektArbeidYtelseGrunnlagRequest initRequest(Behandling behandling) {
        var request = new InntektArbeidYtelseGrunnlagRequest(new AktørIdPersonident(behandling.getAktørId().getId()));
        request.medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        request.medYtelseType(YtelseType.fraKode(behandling.getFagsakYtelseType().getKode()));
        request.forKobling(behandling.getUuid());
        request.medDataset(Arrays.asList(Dataset.values()));
        return request;
    }
}
