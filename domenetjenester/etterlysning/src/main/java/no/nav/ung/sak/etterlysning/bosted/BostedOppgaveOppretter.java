package no.nav.ung.sak.etterlysning.bosted;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bosted.BekreftBostedOppgavetypeDataDto;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;

import java.util.List;
import java.util.Objects;

@Dependent
public class BostedOppgaveOppretter {

    private final UngBrukerdialogOppgaveKlient oppgaveKlient;
    private final BostedsGrunnlagRepository bostedsGrunnlagRepository;

    @Inject
    public BostedOppgaveOppretter(UngBrukerdialogOppgaveKlient oppgaveKlient,
                                  BostedsGrunnlagRepository bostedsGrunnlagRepository) {
        this.oppgaveKlient = oppgaveKlient;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());

        for (Etterlysning etterlysning : etterlysninger) {
            var periodeAvklaring = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId())
                .stream()
                .flatMap(g -> g.getForeslått().getPeriodeAvklaring(etterlysning.getGrunnlagsreferanse()).stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fant ikke periodeAvklaring for referanse: " + etterlysning.getGrunnlagsreferanse()));

            var ikkeOppfyltÅrsak = periodeAvklaring.getIkkeOppfyltÅrsak();
            if (ikkeOppfyltÅrsak == BostedsvilkårIkkeOppfyltÅrsak.ANNET || ikkeOppfyltÅrsak == BostedsvilkårIkkeOppfyltÅrsak.UDEFINERT) {
                Objects.requireNonNull(periodeAvklaring.getFritekstTilVarsel(), "FritekstTilVarsel må være satt når årsak er "+ ikkeOppfyltÅrsak);
            }

            var oppgaveDto = new OpprettOppgaveDto(
                new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
                ytelsetype,
                etterlysning.getEksternReferanse(),
                new BekreftBostedOppgavetypeDataDto(
                    etterlysning.getPeriode().getFomDato(),
                    etterlysning.getPeriode().getTomDato(),
                    periodeAvklaring.isErBosattITrondheim(),
                    periodeAvklaring.getFritekstTilVarsel(),
                    mapIkkeOppfyltÅrsak(ikkeOppfyltÅrsak)
                ),
                etterlysning.getFrist()
            );
            oppgaveKlient.opprettOppgave(oppgaveDto);
        }
    }

    static no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bosted.BostedsvilkårIkkeOppfyltÅrsak mapIkkeOppfyltÅrsak(BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak) {
        return switch (ikkeOppfyltÅrsak) {
            case IKKE_BOSATTADRESSE_I_TRONDHEIM -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bosted.BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM;
            case IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bosted.BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM;
            case STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bosted.BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM;
            case ANNET -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bosted.BostedsvilkårIkkeOppfyltÅrsak.ANNET;
            case UDEFINERT -> no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bosted.BostedsvilkårIkkeOppfyltÅrsak.UDEFINERT;
        };
    }
}
