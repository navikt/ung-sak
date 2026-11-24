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
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelse;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelseRepository;
import no.nav.ung.sak.behandlingslager.samordning.YtelseAnvist;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningPeriode;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningVedtak;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningVedtakRequest;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningVedtakResponse;
import no.nav.ung.sak.kontrakt.ekstern.samordning.SamordningYtelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class SamordningYtelseTjenesteTest {

    private static final PersonIdent FNR = new PersonIdent("12345678901");
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
        tjeneste = new SamordningYtelseTjeneste(vedtakYtelseRepository, aktørTjeneste);
        aktørId = AktørId.dummy();
        when(aktørTjeneste.hentAlleAktørIderForPersonIdent(FNR)).thenReturn(Set.of(aktørId));
    }

    @Test
    void skal_returnere_gjeldende_vedtak_med_perioder_og_barnetillegg() {
        var fagsak = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER, aktørId);
        var vedtakReferanse = UUID.randomUUID();
        lagreVedtak(fagsak, vedtakReferanse, VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, MAR_TOM),
            beregnet(JAN_FOM, JAN_TOM, "800", "9600", 2, 74),
            ikkeBeregnet(FEB_FOM, MAR_TOM, 2, 74));

        var resultat = tjeneste.hentVedtak(forespørsel(JAN_FOM, APR_TOM));

        assertThat(VALIDATOR.validate(resultat)).isEmpty();
        assertThat(resultat.vedtak()).singleElement().satisfies(vedtak -> {
            assertThat(vedtak.ytelse()).isEqualTo(SamordningYtelse.AKTIVITETSPENGER);
            assertThat(vedtak.saksnummer()).isEqualTo(fagsak.getSaksnummer().getVerdi());
            assertThat(vedtak.vedtakReferanse()).isEqualTo(vedtakReferanse);
            assertThat(vedtak.vedtattTidspunkt()).isEqualTo(VEDTATT);
            assertThat(vedtak.perioder()).usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(
                    new SamordningPeriode(JAN_FOM, JAN_TOM, new BigDecimal("800"), new BigDecimal("9600"), 2, 74),
                    new SamordningPeriode(FEB_FOM, MAR_TOM, null, null, 2, 74)));
        });
    }

    @Test
    void skal_returnere_perioder_med_0_kr() {
        var fagsak = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE, aktørId);
        lagreVedtak(fagsak, UUID.randomUUID(), VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM),
            beregnet(JAN_FOM, JAN_TOM, "0", "0", 0, 0));

        var periode = enestePeriode(tjeneste.hentVedtak(forespørsel(JAN_FOM, JAN_TOM)));

        assertThat(periode.dagsats()).isZero();
        assertThat(periode.beløp()).isZero();
    }

    @Test
    void skal_slå_opp_alle_aktørIder_for_fnr() {
        var historiskAktørId = AktørId.dummy();
        when(aktørTjeneste.hentAlleAktørIderForPersonIdent(FNR)).thenReturn(Set.of(aktørId, historiskAktørId));
        lagreVedtak(lagFagsak(FagsakYtelseType.UNGDOMSYTELSE, aktørId), UUID.randomUUID(), VEDTATT,
            DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM), ikkeBeregnet(JAN_FOM, JAN_TOM));
        lagreVedtak(lagFagsak(FagsakYtelseType.AKTIVITETSPENGER, historiskAktørId), UUID.randomUUID(), VEDTATT,
            DatoIntervallEntitet.fraOgMedTilOgMed(FEB_FOM, MAR_TOM), ikkeBeregnet(FEB_FOM, MAR_TOM));

        var resultat = tjeneste.hentVedtak(forespørsel(JAN_FOM, APR_TOM));

        assertThat(resultat.vedtak()).extracting(SamordningVedtak::ytelse)
            .containsExactlyInAnyOrder(SamordningYtelse.UNGDOMSPROGRAMYTELSE, SamordningYtelse.AKTIVITETSPENGER);
    }

    @Test
    void skal_returnere_tomt_svar_når_person_er_ukjent() {
        when(aktørTjeneste.hentAlleAktørIderForPersonIdent(FNR)).thenReturn(Set.of());

        assertThat(tjeneste.hentVedtak(forespørsel(JAN_FOM, APR_TOM)).vedtak()).isEmpty();
    }

    @Test
    void skal_ikke_returnere_andre_ytelser() {
        lagreVedtak(lagFagsak(FagsakYtelseType.OMSORGSPENGER, aktørId), UUID.randomUUID(), VEDTATT,
            DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM), ikkeBeregnet(JAN_FOM, JAN_TOM));

        assertThat(tjeneste.hentVedtak(forespørsel(JAN_FOM, APR_TOM)).vedtak()).isEmpty();
    }

    @Test
    void skal_ikke_returnere_noe_når_innvilgelse_er_revurdert_til_avslag() {
        var fagsak = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER, aktørId);
        lagreVedtak(fagsak, UUID.randomUUID(), VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, MAR_TOM), ikkeBeregnet(JAN_FOM, MAR_TOM));
        var avslagsdato = LocalDate.of(2026, 2, 10);
        lagreVedtak(fagsak, UUID.randomUUID(), avslagsdato.atStartOfDay(), DatoIntervallEntitet.fraOgMedTilOgMed(avslagsdato, avslagsdato));

        var resultat = tjeneste.hentVedtak(forespørsel(JAN_FOM, APR_TOM));

        assertThat(resultat.vedtak()).isEmpty();
    }

    @Test
    void skal_kun_returnere_gjeldende_vedtak() {
        var fagsak = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE, aktørId);
        lagreVedtak(fagsak, UUID.randomUUID(), VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, JAN_TOM), ikkeBeregnet(JAN_FOM, JAN_TOM));
        var gjeldende = UUID.randomUUID();
        lagreVedtak(fagsak, gjeldende, VEDTATT.plusDays(10), DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, MAR_TOM), ikkeBeregnet(JAN_FOM, MAR_TOM));

        var resultat = tjeneste.hentVedtak(forespørsel(JAN_FOM, APR_TOM));

        assertThat(resultat.vedtak()).extracting(SamordningVedtak::vedtakReferanse).containsExactly(gjeldende);
    }

    @Test
    void skal_ikke_returnere_vedtak_når_forespørselen_kun_treffer_hull_mellom_periodene() {
        var fagsak = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER, aktørId);
        lagreVedtak(fagsak, UUID.randomUUID(), VEDTATT, DatoIntervallEntitet.fraOgMedTilOgMed(JAN_FOM, APR_TOM),
            ikkeBeregnet(JAN_FOM, JAN_TOM),
            ikkeBeregnet(APR_FOM, APR_TOM));

        var iHullet = tjeneste.hentVedtak(forespørsel(FEB_FOM, MAR_TOM));
        var overlapperPeriode = tjeneste.hentVedtak(forespørsel(MAR_TOM, APR_FOM));

        assertThat(iHullet.vedtak()).isEmpty();
        assertThat(overlapperPeriode.vedtak()).singleElement()
            .satisfies(vedtak -> assertThat(vedtak.perioder()).as("periodene kuttes ikke").hasSize(2));
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

    private static YtelseAnvist beregnet(LocalDate fom, LocalDate tom, String dagsats, String beløp, int antallBarn, int dagsatsBarnetillegg) {
        return YtelseAnvist.builder(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medDagsats(new BigDecimal(dagsats))
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medBeløp(new BigDecimal(beløp))
            .medAntallBarn(antallBarn)
            .medDagsatsBarnetillegg(dagsatsBarnetillegg)
            .build();
    }

    private static YtelseAnvist ikkeBeregnet(LocalDate fom, LocalDate tom) {
        return YtelseAnvist.builder(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)).build();
    }

    private static YtelseAnvist ikkeBeregnet(LocalDate fom, LocalDate tom, int antallBarn, int dagsatsBarnetillegg) {
        return YtelseAnvist.builder(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medAntallBarn(antallBarn)
            .medDagsatsBarnetillegg(dagsatsBarnetillegg)
            .build();
    }

    private static SamordningVedtakRequest forespørsel(LocalDate fom, LocalDate tom) {
        return new SamordningVedtakRequest(FNR, fom, tom);
    }

    private static SamordningPeriode enestePeriode(SamordningVedtakResponse resultat) {
        assertThat(resultat.vedtak()).hasSize(1);
        List<SamordningPeriode> perioder = resultat.vedtak().getFirst().perioder();
        assertThat(perioder).hasSize(1);
        return perioder.getFirst();
    }
}
