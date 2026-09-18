package no.nav.ung.sak.etterlysning.livsopphold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.journalforing.JournalføringDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.BekreftAndreLivsoppholdsytelserOpphørOppgavetypeDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.BekreftAndreLivsoppholdsytelserOppgavetypeDataDto;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;

import java.util.List;
import java.util.Objects;

@Dependent
public class AndreLivsoppholdsytelserOppgaveOppretter {

    private final UngBrukerdialogOppgaveKlient oppgaveKlient;
    private final VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;

    @Inject
    public AndreLivsoppholdsytelserOppgaveOppretter(UngBrukerdialogOppgaveKlient oppgaveKlient,
                                                    VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository) {
        this.oppgaveKlient = oppgaveKlient;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());

        for (Etterlysning etterlysning : etterlysninger) {
            var periodeAvklaring = vilkårsavklaringGrunnlagRepository
                .hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR)
                .stream()
                .flatMap(g -> g.getForeslåtteAvklaringer().stream())
                .filter(avklaring -> avklaring.getReferanse().equals(etterlysning.getGrunnlagsreferanse()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fant ikke periodeAvklaring for referanse: " + etterlysning.getGrunnlagsreferanse()));

            var ikkeOppfyltÅrsak = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.fraKode(periodeAvklaring.getIkkeOppfyltÅrsakKode());
            if (ikkeOppfyltÅrsak.kreverFritekst()) {
                Objects.requireNonNull(periodeAvklaring.getFritekstTilVarsel(), "FritekstTilVarsel må være satt når årsak er " + ikkeOppfyltÅrsak);
            }

            var mappetÅrsak = mapIkkeOppfyltÅrsak(ikkeOppfyltÅrsak);
            var mappetKilde = mapKilde(AndreLivsoppholdsytelserAvklaringKildeType.fraKode(periodeAvklaring.getKildeKode()));

            OppgavetypeDataDto oppgavetypeData;
            if (periodeAvklaring.getAvklaringtype() == Avklaringtype.OPPHØR) {
                oppgavetypeData = new BekreftAndreLivsoppholdsytelserOpphørOppgavetypeDataDto(
                    etterlysning.getPeriode().getFomDato(),
                    mappetÅrsak,
                    periodeAvklaring.getFritekstTilVarsel(),
                    mappetKilde,
                    periodeAvklaring.getKildeFritekst()
                );
            } else {
                oppgavetypeData = new BekreftAndreLivsoppholdsytelserOppgavetypeDataDto(
                    etterlysning.getPeriode().getFomDato(),
                    etterlysning.getPeriode().getTomDato(),
                    mappetÅrsak,
                    periodeAvklaring.getFritekstTilVarsel(),
                    mappetKilde,
                    periodeAvklaring.getKildeFritekst()
                );
            }

            var oppgaveDto = new OpprettOppgaveDto(
                new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
                ytelsetype,
                etterlysning.getEksternReferanse(),
                oppgavetypeData,
                etterlysning.getFrist(),
                new JournalføringDto(new no.nav.ung.brukerdialog.typer.Saksnummer(behandling.getFagsak().getSaksnummer().getVerdi()))
            );
            oppgaveKlient.opprettOppgave(oppgaveDto);
        }
    }

    static no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak mapIkkeOppfyltÅrsak(AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak) {
        return switch (ikkeOppfyltÅrsak) {
            case MOTTAR_ARBEIDSAVKLARINGSPENGER ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ARBEIDSAVKLARINGSPENGER;
            case MOTTAR_TILTAKSPENGER ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_TILTAKSPENGER;
            case MOTTAR_KVALIFISERINGSSTØNAD ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_KVALIFISERINGSSTØNAD;
            case MOTTAR_DAGPENGER ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER;
            case MOTTAR_FORELDREPENGER ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_FORELDREPENGER;
            case MOTTAR_SVANGERSKAPSPENGER ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_SVANGERSKAPSPENGER;
            case MOTTAR_UFØRETRYGD ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_UFØRETRYGD;
            case MOTTAR_INTRODUKSJONSSTØNAD ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_INTRODUKSJONSSTØNAD;
            case MOTTAR_BARNEPENSJON ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_BARNEPENSJON;
            case MOTTAR_ANNEN_YTELSE ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE;
            case AVKORTET -> throw new IllegalStateException("Det er ikke forventet at AVKORTET skal brukes på periode det skal varsles om. Det er antagelig feil i løsningen som gjør at saksbehandler kan sette denne årsaken her.");
            case UDEFINERT -> throw new IllegalArgumentException("Ikke-støttet årsak for varsling: " + ikkeOppfyltÅrsak);

        };
    }

    static no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserAvklaringKildeType mapKilde(AndreLivsoppholdsytelserAvklaringKildeType kilde) {
        return switch (kilde) {
            case BRUKER ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserAvklaringKildeType.BRUKER;
            case NAV ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserAvklaringKildeType.NAV;
            case ANNET ->
                no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserAvklaringKildeType.ANNET;
        };
    }
}
