package no.nav.ung.sak.etterlysning.bistand;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.journalforing.JournalføringDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BekreftBistandOppgavetypeDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BekreftBistandOpphørOppgavetypeDataDto;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
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
public class BistandOppgaveOppretter {

    private final UngBrukerdialogOppgaveKlient oppgaveKlient;
    private final VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;

    @Inject
    public BistandOppgaveOppretter(UngBrukerdialogOppgaveKlient oppgaveKlient,
                                   VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository) {
        this.oppgaveKlient = oppgaveKlient;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());

        for (Etterlysning etterlysning : etterlysninger) {
            var periodeAvklaring = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BISTANDSVILKÅR)
                .stream()
                .flatMap(g -> g.getForeslåtteAvklaringer().stream())
                .filter(avklaring -> avklaring.getReferanse().equals(etterlysning.getGrunnlagsreferanse()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fant ikke periodeAvklaring for referanse: " + etterlysning.getGrunnlagsreferanse()));

            var ikkeOppfyltÅrsak = BistandsvilkårIkkeOppfyltÅrsak.fraKode(periodeAvklaring.getIkkeOppfyltÅrsakKode());
            if (ikkeOppfyltÅrsak == BistandsvilkårIkkeOppfyltÅrsak.AVKORTET) {
                throw new IllegalStateException("Det er ikke forventet at AVKORTET skal brukes på periode det skal varsles om. Det er antagelig feil i løsningen som gjør at saksbehandler kan sette denne årsaken her.");
            }
            if (ikkeOppfyltÅrsak.kreverFritekst()) {
                Objects.requireNonNull(periodeAvklaring.getFritekstTilVarsel(), "FritekstTilVarsel må være satt når årsak er " + ikkeOppfyltÅrsak);
            }

            var mappetÅrsak = mapIkkeOppfyltÅrsak(ikkeOppfyltÅrsak);
            var mappetKilde = mapKilde(BistandsavklaringKildeType.fraKode(periodeAvklaring.getKildeKode()));

            OppgavetypeDataDto oppgavetypeData;
            if (periodeAvklaring.getAvklaringtype() == Avklaringtype.OPPHØR) {
                oppgavetypeData = new BekreftBistandOpphørOppgavetypeDataDto(
                    etterlysning.getPeriode().getFomDato(),
                    mappetÅrsak,
                    periodeAvklaring.getFritekstTilVarsel(),
                    mappetKilde,
                    periodeAvklaring.getKildeFritekst()
                );
            } else {
                oppgavetypeData = new BekreftBistandOppgavetypeDataDto(
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

    static no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsvilkårIkkeOppfyltÅrsak mapIkkeOppfyltÅrsak(BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak) {
        return switch (ikkeOppfyltÅrsak) {
            case IKKE_14A_VEDTAK -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK;
            case KOMMET_I_UTDANNING -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_UTDANNING;
            case KOMMET_I_ARBEID -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID;
            case ANNET -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsvilkårIkkeOppfyltÅrsak.ANNET;
            case UDEFINERT, AVKORTET -> throw new IllegalArgumentException("Ikke-støttet årsak for varsling: " + ikkeOppfyltÅrsak);
        };
    }

    static no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsavklaringKildeType mapKilde(BistandsavklaringKildeType kilde) {
        return switch (kilde) {
            case BRUKER -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsavklaringKildeType.BRUKER;
            case NAV -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsavklaringKildeType.NAV;
            case ANNET -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsavklaringKildeType.ANNET;
        };
    }
}
