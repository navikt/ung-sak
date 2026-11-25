package no.nav.ung.sak.web.app.tjenester.ekstern.ytelse;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.BarnetilleggResponse;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.Ytelse;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseRequest;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseResponse;
import no.nav.ung.sak.kontrakt.ekstern.ytelse.YtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.vedtattytelse.VedtakYtelseTjeneste;
import no.nav.ung.sak.web.server.abac.EksternSystemAuditlogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EksternYtelseRestTjenesteTest {

    private static final String KONSUMENT = "dev-gcp:aap:behandlingsflyt";
    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 12, 31);

    private final AktørTjeneste aktørTjeneste = mock(AktørTjeneste.class);
    private final VedtakYtelseTjeneste vedtakYtelseTjeneste = mock(VedtakYtelseTjeneste.class);
    private final EksternSystemAuditlogger auditlogger = mock(EksternSystemAuditlogger.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final EksternYtelseRestTjeneste tjeneste = new EksternYtelseRestTjeneste(aktørTjeneste, vedtakYtelseTjeneste, auditlogger, meterRegistry);

    private final PersonIdent ident = new PersonIdent("12345678901");
    private final Set<AktørId> aktørIder = Set.of(AktørId.dummy(), AktørId.dummy());
    private final YtelseRequest request = new YtelseRequest(ident, FOM, TOM);

    @BeforeEach
    void setUp() {
        when(auditlogger.kallendeSystem()).thenReturn(KONSUMENT);
        when(aktørTjeneste.hentAlleAktørIderForPersonIdent(ident)).thenReturn(aktørIder);
    }

    @Test
    void skal_hente_vedtak_for_alle_aktørIder_og_auditlogge_oppslaget() {
        var vedtak = new YtelseResponse(List.of(new Ytelse(YtelseType.AKTIVITETSPENGER, "SAK1", UUID.randomUUID(), LocalDateTime.now(), List.of())));
        when(vedtakYtelseTjeneste.hentVedtak(aktørIder, new Periode(FOM, TOM))).thenReturn(vedtak);

        var resultat = tjeneste.hentVedtak(request);

        assertThat(resultat).isSameAs(vedtak);
        verify(auditlogger).loggOppslag(KONSUMENT, "/ekstern/ytelse/v1/vedtak", ident);
        assertThat(antallOppslag("/v1/vedtak", "true")).isEqualTo(1.0);
    }

    @Test
    void skal_hente_barnetillegg_og_auditlogge_oppslaget() {
        when(vedtakYtelseTjeneste.hentBarnetillegg(aktørIder, new Periode(FOM, TOM))).thenReturn(new BarnetilleggResponse(List.of()));

        var resultat = tjeneste.hentBarnetillegg(request);

        assertThat(resultat.vedtak()).isEmpty();
        verify(auditlogger).loggOppslag(KONSUMENT, "/ekstern/ytelse/v1/barnetillegg", ident);
        assertThat(antallOppslag("/v1/barnetillegg", "false")).isEqualTo(1.0);
    }

    @Test
    void skal_auditlogge_og_svare_med_tom_liste_når_personen_er_ukjent() {
        when(aktørTjeneste.hentAlleAktørIderForPersonIdent(ident)).thenReturn(Set.of());
        when(vedtakYtelseTjeneste.hentVedtak(Set.of(), new Periode(FOM, TOM))).thenReturn(new YtelseResponse(List.of()));

        var resultat = tjeneste.hentVedtak(request);

        assertThat(resultat.vedtak()).isEmpty();
        verify(auditlogger).loggOppslag(KONSUMENT, "/ekstern/ytelse/v1/vedtak", ident);
        assertThat(antallOppslag("/v1/vedtak", "false")).isEqualTo(1.0);
    }

    private double antallOppslag(String endepunkt, String treff) {
        var teller = meterRegistry.find(EksternYtelseRestTjeneste.METRIKK)
            .tags("endepunkt", endepunkt, "konsument", KONSUMENT, "treff", treff)
            .counter();
        return teller == null ? 0 : teller.count();
    }
}
