package no.nav.ung.sak.web.app.tjenester.ekstern.ytelse;

import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.BarnetilleggResponse;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.BarnetilleggVedtak;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.Ytelse;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseRequest;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseResponse;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.vedtattytelse.VedtakYtelseTjeneste;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EksternYtelseRestTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 12, 31);
    private static final String KONSUMENT = "dev-gcp:tiltakspenger:tiltakspenger-saksbehandling-api";

    private final AktørTjeneste aktørTjeneste = mock(AktørTjeneste.class);
    private final VedtakYtelseTjeneste vedtakYtelseTjeneste = mock(VedtakYtelseTjeneste.class);
    private final EksternYtelseRestTjeneste tjeneste = new EksternYtelseRestTjeneste(aktørTjeneste, vedtakYtelseTjeneste);

    private final PersonIdent ident = new PersonIdent("12345678901");
    private final Set<AktørId> aktørIder = Set.of(AktørId.dummy(), AktørId.dummy());
    private final YtelseRequest request = new YtelseRequest(ident, FOM, TOM);

    @BeforeEach
    void setUp() {
        when(aktørTjeneste.hentAlleAktørIderForPersonIdent(ident)).thenReturn(aktørIder);
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(KONSUMENT);
    }

    @AfterEach
    void tearDown() {
        SubjectHandlerUtils.reset();
        SubjectHandlerUtils.unsetSubjectHandler();
        MDC.clear();
    }

    @Test
    void skal_hente_vedtak_for_alle_aktørIder() {
        var vedtak = new YtelseResponse(List.of(new Ytelse(YtelseType.AKTIVITETSPENGER, "SAK1", UUID.randomUUID(), LocalDateTime.now(), List.of())));
        when(vedtakYtelseTjeneste.hentVedtak(aktørIder, new Periode(FOM, TOM))).thenReturn(vedtak);

        var resultat = tjeneste.hentVedtak(request);

        assertThat(resultat).isSameAs(vedtak);
    }

    @Test
    void skal_legge_konsument_endepunkt_og_saksnumre_i_mdc_ved_oppslag_av_vedtak() {
        when(vedtakYtelseTjeneste.hentVedtak(aktørIder, new Periode(FOM, TOM))).thenReturn(new YtelseResponse(List.of(
            new Ytelse(YtelseType.UNGDOMSPROGRAMYTELSE, "SAK2", UUID.randomUUID(), LocalDateTime.now(), List.of()),
            new Ytelse(YtelseType.AKTIVITETSPENGER, "SAK1", UUID.randomUUID(), LocalDateTime.now(), List.of()))));

        tjeneste.hentVedtak(request);

        assertThat(MDC.get("prosess_konsument")).isEqualTo(KONSUMENT);
        assertThat(MDC.get("prosess_endepunkt")).isEqualTo("/ekstern/ytelse/v1/vedtak");
        assertThat(MDC.get("prosess_saksnummer")).isEqualTo("SAK1,SAK2");
    }

    @Test
    void skal_legge_saksnummer_i_mdc_ved_oppslag_av_barnetillegg() {
        when(vedtakYtelseTjeneste.hentBarnetillegg(aktørIder, new Periode(FOM, TOM))).thenReturn(new BarnetilleggResponse(List.of(
            new BarnetilleggVedtak(YtelseType.AKTIVITETSPENGER, "SAK1", UUID.randomUUID(), LocalDateTime.now(), List.of()))));

        tjeneste.hentBarnetillegg(request);

        assertThat(MDC.get("prosess_konsument")).isEqualTo(KONSUMENT);
        assertThat(MDC.get("prosess_endepunkt")).isEqualTo("/ekstern/ytelse/v1/barnetillegg");
        assertThat(MDC.get("prosess_saksnummer")).isEqualTo("SAK1");
    }

    @Test
    void skal_ikke_legge_saksnummer_i_mdc_uten_treff() {
        when(vedtakYtelseTjeneste.hentVedtak(aktørIder, new Periode(FOM, TOM))).thenReturn(new YtelseResponse(List.of()));

        tjeneste.hentVedtak(request);

        assertThat(MDC.get("prosess_konsument")).isEqualTo(KONSUMENT);
        assertThat(MDC.get("prosess_saksnummer")).isNull();
    }

    @Test
    void skal_hente_barnetillegg() {
        when(vedtakYtelseTjeneste.hentBarnetillegg(aktørIder, new Periode(FOM, TOM))).thenReturn(new BarnetilleggResponse(List.of()));

        var resultat = tjeneste.hentBarnetillegg(request);

        assertThat(resultat.vedtak()).isEmpty();
    }

    @Test
    void skal_gi_fnr_som_abac_attributt_slik_at_gjenbruk_av_requesten_får_personsjekk() {
        var gyldigRequest = new YtelseRequest(new PersonIdent("01419010029"), FOM, TOM);

        var attributter = new AbacAttributtSupplier().apply(gyldigRequest);

        assertThat(attributter.<String>getVerdier(StandardAbacAttributtType.FNR)).containsExactly("01419010029");
    }

    @Test
    void skal_svare_med_tom_liste_når_personen_er_ukjent() {
        when(aktørTjeneste.hentAlleAktørIderForPersonIdent(ident)).thenReturn(Set.of());
        when(vedtakYtelseTjeneste.hentVedtak(Set.of(), new Periode(FOM, TOM))).thenReturn(new YtelseResponse(List.of()));

        var resultat = tjeneste.hentVedtak(request);

        assertThat(resultat.vedtak()).isEmpty();
    }
}
