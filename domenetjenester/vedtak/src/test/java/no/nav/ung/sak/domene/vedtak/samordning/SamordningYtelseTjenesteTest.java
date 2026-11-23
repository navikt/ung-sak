package no.nav.ung.sak.domene.vedtak.samordning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Status;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.request.VedtakForPeriodeRequest;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelse;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelseRepository;
import no.nav.ung.sak.behandlingslager.samordning.YtelseAnvist;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class SamordningYtelseTjenesteTest {

    private static final String FNR = "12345678901";
    private static final LocalDate JAN_FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_TOM = LocalDate.of(2026, 1, 31);
    private static final LocalDate FEB_FOM = LocalDate.of(2026, 2, 1);
    private static final LocalDate MAR_TOM = LocalDate.of(2026, 3, 31);
    private static final LocalDate APR_FOM = LocalDate.of(2026, 4, 1);
    private static final LocalDate APR_TOM = LocalDate.of(2026, 4, 30);
    private static final LocalDateTime VEDTATT = LocalDateTime.of(2026, 1, 5, 12, 0);

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Inject
    private EntityManager entityManager;

    private final AktørTjeneste aktørTjeneste = mock();

    private FagsakRepository fagsakRepository;
    private VedtakYtelseRepository vedtakYtelseRepository;
    private SamordningYtelseTjeneste tjeneste;
    private AktørId aktørId;

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(entityManager);
        vedtakYtelseRepository = new VedtakYtelseRepository(entityManager);
        tjeneste = new SamordningYtelseTjeneste(vedtakYtelseRepository, fagsakRepository, aktørTjeneste);
        aktørId = AktørId.dummy();
    }

    @Test
    void skal_returnere_gjeldende_vedtak_i_abakus_format_som_validerer() {
        var fagsak = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER, aktørId);
        fagsakRepository.oppdaterFagsakStatus(fagsak.getId(), FagsakStatus.LØPENDE);
        var vedtakReferanse = UUID.randomUUID();
        lagreVedtak(fagsak, vedtakReferanse, VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, MAR_TOM),
            beregnet(JAN_FOM, JAN_TOM, "800", "60", "9600"),
            ikkeBeregnet(FEB_FOM, MAR_TOM));

        var resultat = tjeneste.hentVedtak(forespørsel(aktørId.getId(), JAN_FOM, APR_TOM));

        assertThat(resultat).singleElement().satisfies(ytelse -> {
            assertThat(VALIDATOR.validate(ytelse)).isEmpty();
            assertThat(ytelse.getAktør().getVerdi()).isEqualTo(aktørId.getId());
            assertThat(ytelse.getSaksnummer()).isEqualTo(fagsak.getSaksnummer().getVerdi());
            assertThat(ytelse.getYtelse()).isEqualTo(Ytelser.AKTIVITETSPENGER);
            assertThat(ytelse.getYtelseStatus()).isEqualTo(Status.LØPENDE);
            assertThat(ytelse.getKildesystem()).isEqualTo(Kildesystem.UNGSAK);
            assertThat(ytelse.getVedtakReferanse()).isEqualTo(vedtakReferanse.toString());
            assertThat(ytelse.getVedtattTidspunkt()).isEqualTo(VEDTATT);
            assertThat(ytelse.getPeriode().getFom()).isEqualTo(JAN_FOM);
            assertThat(ytelse.getPeriode().getTom()).isEqualTo(MAR_TOM);
            assertThat(ytelse.getAnvist()).hasSize(2);
        });
    }

    @Test
    void skal_mappe_beregnet_anvisning_med_andel_også_når_dagsats_er_0() {
        var fagsak = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE, aktørId);
        lagreVedtak(fagsak, UUID.randomUUID(), VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM),
            beregnet(JAN_FOM, JAN_TOM, "0", "0", "0"));

        var anvisning = enesteAnvisning(tjeneste.hentVedtak(forespørsel(aktørId.getId(), JAN_FOM, JAN_TOM)));

        assertThat(anvisning.getDagsats().getVerdi()).isZero();
        assertThat(anvisning.getBeløp().getVerdi()).isZero();
        assertThat(anvisning.getAndeler()).singleElement()
            .satisfies(andel -> assertThat(andel.getDagsats().getVerdi()).isZero());
    }

    @Test
    void skal_mappe_ikke_beregnet_anvisning_uten_dagsats_og_andeler() {
        var fagsak = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE, aktørId);
        lagreVedtak(fagsak, UUID.randomUUID(), VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(FEB_FOM, MAR_TOM),
            ikkeBeregnet(FEB_FOM, MAR_TOM));

        var anvisning = enesteAnvisning(tjeneste.hentVedtak(forespørsel(aktørId.getId(), FEB_FOM, MAR_TOM)));

        assertThat(anvisning.getDagsats()).isNull();
        assertThat(anvisning.getBeløp()).isNull();
        assertThat(anvisning.getUtbetalingsgrad()).isNull();
        assertThat(anvisning.getAndeler()).isEmpty();
    }

    @Test
    void skal_slå_opp_alle_aktørIder_for_fnr() {
        var historiskAktørId = AktørId.dummy();
        when(aktørTjeneste.hentAlleAktørIderForPersonIdent(new PersonIdent(FNR))).thenReturn(Set.of(aktørId, historiskAktørId));
        lagreVedtak(lagFagsak(FagsakYtelseType.UNGDOMSYTELSE, aktørId), UUID.randomUUID(), VEDTATT,
            DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM), ikkeBeregnet(JAN_FOM, JAN_TOM));
        lagreVedtak(lagFagsak(FagsakYtelseType.AKTIVITETSPENGER, historiskAktørId), UUID.randomUUID(), VEDTATT,
            DatoIntervallEntitet.fraOgMedTilOgMed(FEB_FOM, MAR_TOM), ikkeBeregnet(FEB_FOM, MAR_TOM));

        var resultat = tjeneste.hentVedtak(forespørsel(FNR, JAN_FOM, APR_TOM));

        assertThat(resultat).extracting(YtelseV1::getYtelse).containsExactlyInAnyOrder(Ytelser.UNGDOMSYTELSE, Ytelser.AKTIVITETSPENGER);
    }

    @Test
    void skal_returnere_begge_ytelser_når_ytelser_ikke_er_oppgitt() {
        lagreInnvilgelse(FagsakYtelseType.UNGDOMSYTELSE);
        lagreInnvilgelse(FagsakYtelseType.AKTIVITETSPENGER);

        var resultat = tjeneste.hentVedtak(VedtakForPeriodeRequest.requestFor(aktørId.getId(), JAN_FOM, JAN_TOM));

        assertThat(resultat).extracting(YtelseV1::getYtelse).containsExactlyInAnyOrder(Ytelser.UNGDOMSYTELSE, Ytelser.AKTIVITETSPENGER);
    }

    @Test
    void skal_filtrere_på_oppgitte_ytelser() {
        lagreInnvilgelse(FagsakYtelseType.UNGDOMSYTELSE);
        lagreInnvilgelse(FagsakYtelseType.AKTIVITETSPENGER);

        var kunAvp = tjeneste.hentVedtak(VedtakForPeriodeRequest.requestFor(aktørId.getId(), JAN_FOM, JAN_TOM, Set.of(Ytelser.AKTIVITETSPENGER)));
        var andreYtelser = tjeneste.hentVedtak(VedtakForPeriodeRequest.requestFor(aktørId.getId(), JAN_FOM, JAN_TOM, Set.of(Ytelser.FORELDREPENGER)));

        assertThat(kunAvp).extracting(YtelseV1::getYtelse).containsExactly(Ytelser.AKTIVITETSPENGER);
        assertThat(andreYtelser).isEmpty();
    }

    @Test
    void skal_ikke_returnere_noe_når_innvilgelse_er_revurdert_til_avslag() {
        var fagsak = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER, aktørId);
        lagreVedtak(fagsak, UUID.randomUUID(), VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, MAR_TOM), ikkeBeregnet(JAN_FOM, MAR_TOM));
        var avslagsdato = LocalDate.of(2026, 2, 10);
        lagreVedtak(fagsak, UUID.randomUUID(), avslagsdato.atStartOfDay(), DatoIntervallEntitet.fraOgMedTilOgMed(avslagsdato, avslagsdato));

        var resultat = tjeneste.hentVedtak(forespørsel(aktørId.getId(), JAN_FOM, APR_TOM));

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_kun_returnere_gjeldende_vedtak() {
        var fagsak = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE, aktørId);
        lagreVedtak(fagsak, UUID.randomUUID(), VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM), ikkeBeregnet(JAN_FOM, JAN_TOM));
        var gjeldende = UUID.randomUUID();
        lagreVedtak(fagsak, gjeldende, VEDTATT.plusDays(10), DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, MAR_TOM), ikkeBeregnet(JAN_FOM, MAR_TOM));

        var resultat = tjeneste.hentVedtak(forespørsel(aktørId.getId(), JAN_FOM, APR_TOM));

        assertThat(resultat).extracting(YtelseV1::getVedtakReferanse).containsExactly(gjeldende.toString());
    }

    @Test
    void skal_ikke_returnere_vedtak_når_forespørselen_kun_treffer_hull_mellom_anvisningene() {
        var fagsak = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER, aktørId);
        lagreVedtak(fagsak, UUID.randomUUID(), VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, APR_TOM),
            ikkeBeregnet(JAN_FOM, JAN_TOM),
            ikkeBeregnet(APR_FOM, APR_TOM));

        var iHullet = tjeneste.hentVedtak(forespørsel(aktørId.getId(), FEB_FOM, MAR_TOM));
        var overlapperAnvisning = tjeneste.hentVedtak(forespørsel(aktørId.getId(), MAR_TOM, APR_FOM));

        assertThat(iHullet).isEmpty();
        assertThat(overlapperAnvisning).singleElement()
            .satisfies(ytelse -> assertThat(ytelse.getAnvist()).as("periodene kuttes ikke").hasSize(2));
    }

    @Test
    void skal_tolke_standardverdi_og_tom_liste_for_ytelser_som_begge_ung_ytelser() {
        assertThat(SamordningYtelseTjeneste.utledYtelseTyper(VedtakForPeriodeRequest.ALLE_YTELSER))
            .containsExactlyInAnyOrder(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);
        assertThat(SamordningYtelseTjeneste.utledYtelseTyper(null))
            .containsExactlyInAnyOrder(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);
        assertThat(SamordningYtelseTjeneste.utledYtelseTyper(Set.of()))
            .containsExactlyInAnyOrder(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);
        assertThat(SamordningYtelseTjeneste.utledYtelseTyper(Set.of(Ytelser.UNGDOMSYTELSE, Ytelser.FORELDREPENGER)))
            .containsExactly(FagsakYtelseType.UNGDOMSYTELSE);
    }

    private void lagreInnvilgelse(FagsakYtelseType ytelseType) {
        lagreVedtak(lagFagsak(ytelseType, aktørId), UUID.randomUUID(), VEDTATT,
            DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM), ikkeBeregnet(JAN_FOM, JAN_TOM));
    }

    private Fagsak lagFagsak(FagsakYtelseType ytelseType, AktørId aktør) {
        var saksnummer = new Saksnummer("SAM" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
        var fagsak = Fagsak.opprettNy(ytelseType, aktør, saksnummer);
        fagsakRepository.opprettNy(fagsak);
        entityManager.flush();
        return fagsak;
    }

    private void lagreVedtak(Fagsak fagsak, UUID vedtakReferanse, LocalDateTime vedtatt, DatoIntervallEntitet periode, YtelseAnvist... anvisninger) {
        var builder = VedtakYtelse.builder()
            .medAktørId(fagsak.getAktørId())
            .medSaksnummer(fagsak.getSaksnummer())
            .medYtelseType(fagsak.getYtelseType())
            .medVedtakReferanse(vedtakReferanse)
            .medVedtattTidspunkt(vedtatt)
            .medPeriode(periode);
        for (var anvisning : anvisninger) {
            builder.leggTilAnvisning(anvisning);
        }
        vedtakYtelseRepository.lagre(builder.build());
    }

    private static YtelseAnvist beregnet(LocalDate fom, LocalDate tom, String dagsats, String utbetalingsgrad, String beløp) {
        return YtelseAnvist.builder(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medDagsats(new BigDecimal(dagsats))
            .medUtbetalingsgrad(new BigDecimal(utbetalingsgrad))
            .medBeløp(new BigDecimal(beløp))
            .medAntallBarn(0)
            .medDagsatsBarnetillegg(0)
            .build();
    }

    private static YtelseAnvist ikkeBeregnet(LocalDate fom, LocalDate tom) {
        return YtelseAnvist.builder(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)).build();
    }

    private static VedtakForPeriodeRequest forespørsel(String ident, LocalDate fom, LocalDate tom) {
        return VedtakForPeriodeRequest.requestFor(ident, fom, tom);
    }

    private static Anvisning enesteAnvisning(List<YtelseV1> resultat) {
        assertThat(resultat).hasSize(1);
        assertThat(resultat.getFirst().getAnvist()).hasSize(1);
        return resultat.getFirst().getAnvist().getFirst();
    }
}
