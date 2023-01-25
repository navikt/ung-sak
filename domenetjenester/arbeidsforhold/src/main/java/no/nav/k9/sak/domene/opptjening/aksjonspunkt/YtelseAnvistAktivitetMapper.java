package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static no.nav.k9.sak.domene.opptjening.aksjonspunkt.MapYtelseperioderTjeneste.hentUtDatoIntervall;

import java.util.List;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;

class YtelseAnvistAktivitetMapper {

    private final OpptjeningAktivitetVurdering vurderer;
    private final BehandlingReferanse behandlingReferanse;
    private final DatoIntervallEntitet vilkårsperiode;
    private final OpptjeningAktivitetType opptjeningAktivitetType;
    private final Ytelse ytelse;

    public YtelseAnvistAktivitetMapper(OpptjeningAktivitetVurdering vurderer,
                                       BehandlingReferanse behandlingReferanse,
                                       DatoIntervallEntitet vilkårsperiode,
                                       OpptjeningAktivitetType opptjeningAktivitetType, Ytelse ytelse) {
        this.behandlingReferanse = behandlingReferanse;
        this.vilkårsperiode = vilkårsperiode;
        this.vurderer = vurderer;
        this.opptjeningAktivitetType = opptjeningAktivitetType;
        this.ytelse = ytelse;
    }


    List<OpptjeningsperiodeForSaksbehandling> mapAnvisning(YtelseAnvist ytelseAnvist,
                                                           List<String> orgnumre) {
        if (orgnumre.isEmpty()) {
            return mapAnvisningUtenOrgnr(ytelseAnvist);
        } else {
            return orgnumre.stream().map(orgnr -> mapAnvisningForOrgnr(ytelseAnvist, orgnr)).toList();
        }
    }


    private List<OpptjeningsperiodeForSaksbehandling> mapAnvisningUtenOrgnr(YtelseAnvist ytelseAnvist) {
        var periode = hentUtDatoIntervall(ytelse, ytelseAnvist);
        OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(periode)
            .medOpptjeningAktivitetType(opptjeningAktivitetType)
            .medVurderingsStatus(vurderer.vurderStatus(lagInput(opptjeningAktivitetType, behandlingReferanse, vilkårsperiode, periode)));
        return List.of(builder.build());
    }

    private OpptjeningsperiodeForSaksbehandling mapAnvisningForOrgnr(YtelseAnvist ytelseAnvist,
                                                                     String orgnr) {
        OpptjeningsperiodeForSaksbehandling.Builder builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
            .medPeriode(hentUtDatoIntervall(ytelse, ytelseAnvist))
            .medOpptjeningAktivitetType(opptjeningAktivitetType)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medOpptjeningsnøkkel(Opptjeningsnøkkel.forOrgnummer(orgnr))
            .medVurderingsStatus(vurderer.vurderStatus(lagInput(opptjeningAktivitetType, behandlingReferanse, vilkårsperiode, hentUtDatoIntervall(ytelse, ytelseAnvist))));
        return builder.build();
    }

    private VurderStatusInput lagInput(OpptjeningAktivitetType type, BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsPeriode, DatoIntervallEntitet periode) {
        var input = new VurderStatusInput(type, behandlingReferanse);
        input.setVilkårsperiode(vilkårsPeriode);
        input.setAktivitetPeriode(periode);
        return input;
    }


}
