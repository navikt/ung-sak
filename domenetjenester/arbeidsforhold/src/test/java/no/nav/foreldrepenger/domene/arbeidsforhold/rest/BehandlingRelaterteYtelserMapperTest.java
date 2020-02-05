package no.nav.foreldrepenger.domene.arbeidsforhold.rest;

import static no.nav.foreldrepenger.domene.arbeidsforhold.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER;
import static no.nav.foreldrepenger.domene.arbeidsforhold.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_SØKER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.arbeidsforhold.BehandlingRelaterteYtelserMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.NavBrukerBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.arbeidsforhold.TemaUnderkategori;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.arbeidsforhold.RelaterteYtelserDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.TilgrensendeYtelserDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

public class BehandlingRelaterteYtelserMapperTest {
    private static final LocalDate I_DAG = LocalDate.now();
    private static final Saksnummer SAKSNUMMER_42 = new Saksnummer("42");
    private NavBruker navBruker = new NavBrukerBuilder().medAktørId(AktørId.dummy()).build();
    private Fagsak fagsakFødsel = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, navBruker, new Saksnummer("66"));

    @Test
    public void skal_returnerer_tilgrensende_ytelser_for_soker() {
        List<Ytelse> ytelser = List.of(
            opprettBuilderForBehandlingRelaterteYtelser(FagsakYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettBuilderForBehandlingRelaterteYtelser(FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(265),
                I_DAG.plusDays(200)),
            opprettBuilderForBehandlingRelaterteYtelser(FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5), null));

        List<TilgrensendeYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(ytelser);

        assertThat(resultatListe).hasSize(3);
        assertThat(resultatListe.get(0).getRelatertYtelseType()).isEqualTo(FagsakYtelseType.SYKEPENGER.getKode());
        assertThat(resultatListe.get(0).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(365));
        assertThat(resultatListe.get(1).getRelatertYtelseType()).isEqualTo(FagsakYtelseType.FORELDREPENGER.getKode());
        assertThat(resultatListe.get(1).getPeriodeTilDato()).isEqualTo(I_DAG.plusDays(200));
    }

    @Test
    public void skal_returnerer_tilgrensende_ytelser_for_annen_forelder() {
        List<Ytelse> ytelser = List.of(
            opprettBuilderForBehandlingRelaterteYtelser(FagsakYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettBuilderForBehandlingRelaterteYtelser(FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(265),
                I_DAG.plusDays(200)),
            opprettBuilderForBehandlingRelaterteYtelser(FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.LØPENDE, I_DAG.minusDays(5), null),
            opprettBuilderForBehandlingRelaterteYtelser(FagsakYtelseType.ENSLIG_FORSØRGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5), null));

        List<TilgrensendeYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(ytelser);

        assertThat(resultatListe).hasSize(4);
        assertThat(resultatListe.get(0).getRelatertYtelseType()).isEqualTo(FagsakYtelseType.SYKEPENGER.getKode());
        assertThat(resultatListe.get(0).getStatus()).isEqualTo(RelatertYtelseTilstand.AVSLUTTET.getKode());
        assertThat(resultatListe.get(1).getRelatertYtelseType()).isEqualTo(FagsakYtelseType.FORELDREPENGER.getKode());
        assertThat(resultatListe.get(1).getStatus()).isEqualTo(RelatertYtelseTilstand.AVSLUTTET.getKode());
    }

    @Test
    public void skal_mapper_fagsak_til_tilgrensendeYtelserdto() {
        fagsakFødsel.setAvsluttet();

        TilgrensendeYtelserDto tilgrensendeYtelserDto = BehandlingRelaterteYtelserMapper.mapFraFagsak(fagsakFødsel, I_DAG.minusDays(5));

        assertThat(tilgrensendeYtelserDto.getRelatertYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD.getKode());
        assertThat(tilgrensendeYtelserDto.getStatus()).isEqualTo(FagsakStatus.AVSLUTTET.getKode());
        assertThat(tilgrensendeYtelserDto.getPeriodeTilDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(tilgrensendeYtelserDto.getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(tilgrensendeYtelserDto.getSaksNummer()).isEqualTo("66");
    }

    @Test
    public void skal_returnerer_6_tom_tilgrensende_ytelser_for_soker() {
        @SuppressWarnings("unchecked")
        List<RelaterteYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(Collections.EMPTY_LIST,
            RELATERT_YTELSE_TYPER_FOR_SØKER);

        assertThat(resultatListe).hasSize(7);
        IntStream.range(0, RELATERT_YTELSE_TYPER_FOR_SØKER.size()).forEach(i -> {
            assertThat(resultatListe.get(i).getRelatertYtelseType()).isEqualTo(RELATERT_YTELSE_TYPER_FOR_SØKER.get(i).getKode());
            assertThat(resultatListe.get(i).getTilgrensendeYtelserListe()).isEmpty();
        });
    }

    @Test
    public void skal_returnerer_2_tom_tilgrensende_ytelser_for_soker() {
        @SuppressWarnings("unchecked")
        List<RelaterteYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(Collections.EMPTY_LIST,
            RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER);

        assertThat(resultatListe).hasSize(2);
        IntStream.range(0, RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER.size()).forEach(i -> {
            assertThat(resultatListe.get(i).getRelatertYtelseType()).isEqualTo(RELATERT_YTELSE_TYPER_FOR_SØKER.get(i).getKode());
            assertThat(resultatListe.get(i).getTilgrensendeYtelserListe()).isEmpty();
        });
    }

    @Test
    public void skal_returnerer_sortert_tilgrensende_ytelser_for_soker() {
        List<TilgrensendeYtelserDto> tilgrensendeYtelserDtos = List.of(
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.SYKEPENGER, RelatertYtelseTilstand.LØPENDE, I_DAG.minusDays(265),
                I_DAG.plusDays(260)),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.SYKEPENGER, RelatertYtelseTilstand.LØPENDE, null, null),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(165),
                I_DAG.plusDays(160)),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.SYKEPENGER, RelatertYtelseTilstand.LØPENDE, I_DAG.minusDays(65),
                I_DAG.plusDays(60)),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.SYKEPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5), null),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.ARBEIDSAVKLARINGSPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(500),
                I_DAG.plusDays(400)));

        List<RelaterteYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(tilgrensendeYtelserDtos,
            RELATERT_YTELSE_TYPER_FOR_SØKER);

        assertThat(resultatListe).hasSize(7);
        assertThat(resultatListe.get(0).getRelatertYtelseType()).isEqualTo(FagsakYtelseType.FORELDREPENGER.getKode());
        assertThat(resultatListe.get(0).getTilgrensendeYtelserListe()).hasSize(1);
        assertThat(resultatListe.get(2).getRelatertYtelseType()).isEqualTo(FagsakYtelseType.SYKEPENGER.getKode());
        final List<TilgrensendeYtelserDto> sykepengerYtelserListe = resultatListe.get(2).getTilgrensendeYtelserListe();
        assertThat(sykepengerYtelserListe).hasSize(6);
        assertThat(sykepengerYtelserListe.get(0).getPeriodeFraDato()).isNull();
        assertThat(sykepengerYtelserListe.get(1).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(sykepengerYtelserListe.get(2).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(65));
        assertThat(sykepengerYtelserListe.get(3).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(165));
        assertThat(sykepengerYtelserListe.get(4).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(265));
        assertThat(sykepengerYtelserListe.get(5).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(365));
        assertThat(resultatListe.get(5).getRelatertYtelseType()).isEqualTo(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER.getKode());
        assertThat(resultatListe.get(5).getTilgrensendeYtelserListe()).hasSize(1);
    }

    @Test
    public void skal_returnerer_sortert_tilgrensende_ytelser_for_annen_forelder() {
        List<TilgrensendeYtelserDto> tilgrensendeYtelserDtos = List.of(
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.SYKEPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(365),
                I_DAG.plusDays(360)),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(165),
                I_DAG.plusDays(160)),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.minusDays(5), null),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG.plusMonths(5), null),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, null, null),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.FORELDREPENGER, RelatertYtelseTilstand.ÅPEN, I_DAG, null),
            opprettTilgrensendeYtelserDto(SAKSNUMMER_42, FagsakYtelseType.ENGANGSTØNAD, RelatertYtelseTilstand.AVSLUTTET, I_DAG.minusDays(500),
                I_DAG.plusDays(400)));

        List<RelaterteYtelserDto> resultatListe = BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(tilgrensendeYtelserDtos,
            RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER);

        assertThat(resultatListe).hasSize(2);
        assertThat(resultatListe.get(0).getRelatertYtelseType()).isEqualTo(FagsakYtelseType.FORELDREPENGER.getKode());
        final List<TilgrensendeYtelserDto> foreldrepengerYtelserListe = resultatListe.get(0).getTilgrensendeYtelserListe();
        assertThat(foreldrepengerYtelserListe).hasSize(6);
        assertThat(foreldrepengerYtelserListe.get(0).getPeriodeFraDato()).isNull();
        assertThat(foreldrepengerYtelserListe.get(1).getPeriodeFraDato()).isEqualTo(I_DAG.plusMonths(5));
        assertThat(foreldrepengerYtelserListe.get(2).getPeriodeFraDato()).isEqualTo(I_DAG);
        assertThat(foreldrepengerYtelserListe.get(3).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(5));
        assertThat(foreldrepengerYtelserListe.get(4).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(165));
        assertThat(foreldrepengerYtelserListe.get(5).getPeriodeFraDato()).isEqualTo(I_DAG.minusDays(365));
        assertThat(resultatListe.get(1).getRelatertYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD.getKode());
        assertThat(resultatListe.get(1).getTilgrensendeYtelserListe()).hasSize(1);
    }

    private Ytelse opprettBuilderForBehandlingRelaterteYtelser(FagsakYtelseType ytelseType,
                                                               RelatertYtelseTilstand ytelseTilstand,
                                                               LocalDate iverksettelsesDato,
                                                               LocalDate opphoerFomDato) {

        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var ytelserBuilder = builder.getAktørYtelseBuilder(AktørId.dummy());

        var ytelse = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(Fagsystem.K9SAK)
            .medYtelseType(ytelseType)
            .medSaksnummer(new Saksnummer("22"))
            .medYtelseType(ytelseType)
            .medBehandlingsTema(TemaUnderkategori.UDEFINERT)
            .medStatus(ytelseTilstand)
            .medPeriode(opphoerFomDato == null
                ? DatoIntervallEntitet.fraOgMed(iverksettelsesDato)
                : DatoIntervallEntitet.fraOgMedTilOgMed(iverksettelsesDato, opphoerFomDato));

        ytelserBuilder.leggTilYtelse(ytelse);
        builder.leggTilAktørYtelse(ytelserBuilder);

        return getYtelser(builder).stream().findFirst().orElseThrow();
    }

    private Collection<Ytelse> getYtelser(InntektArbeidYtelseAggregatBuilder builder) {
        return new YtelseFilter(builder.build().getAktørYtelse().iterator().next()).getFiltrertYtelser();
    }

    private TilgrensendeYtelserDto opprettTilgrensendeYtelserDto(Saksnummer saksnummer,
                                                                 FagsakYtelseType ytelseType,
                                                                 RelatertYtelseTilstand ytelseTilstand,
                                                                 LocalDate iverksettelsesDato,
                                                                 LocalDate opphoerFomDato) {
        TilgrensendeYtelserDto tilgrensendeYtelserDto = new TilgrensendeYtelserDto();
        tilgrensendeYtelserDto.setRelatertYtelseType(ytelseType.getKode());
        tilgrensendeYtelserDto.setStatus(ytelseTilstand.getKode());
        tilgrensendeYtelserDto.setSaksNummer(saksnummer);
        tilgrensendeYtelserDto.setPeriodeFraDato(iverksettelsesDato);
        tilgrensendeYtelserDto.setPeriodeTilDato(opphoerFomDato);
        return tilgrensendeYtelserDto;
    }
}
