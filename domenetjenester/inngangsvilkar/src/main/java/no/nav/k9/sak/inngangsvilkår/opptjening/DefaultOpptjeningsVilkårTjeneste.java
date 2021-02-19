package no.nav.k9.sak.inngangsvilkår.opptjening;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Opptjeningsvilkår;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsvilkårResultat;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultOpptjeningsVilkårTjeneste implements OpptjeningsVilkårTjeneste {
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;

    public DefaultOpptjeningsVilkårTjeneste() {
    }

    @Inject
    public DefaultOpptjeningsVilkårTjeneste(InntektArbeidYtelseTjeneste iayTjeneste, OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.opptjeningTjeneste = opptjeningTjeneste;
    }

    @Override
    public NavigableMap<DatoIntervallEntitet, VilkårData> vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse, Collection<DatoIntervallEntitet> perioder) {
        if (perioder.isEmpty()) {
            return Collections.emptyNavigableMap();
        }

        Long behandlingId = behandlingReferanse.getBehandlingId();
        AktørId aktørId = behandlingReferanse.getAktørId();

        NavigableMap<DatoIntervallEntitet, VilkårData> alle = new TreeMap<>();

        var sortertPerioder = new TreeSet<>(perioder);
        var sortertFomDatoer = sortertPerioder.stream().map(DatoIntervallEntitet::getFomDato).collect(Collectors.toList());
        OpptjeningResultat opptjeningResultat = opptjeningTjeneste.hentOpptjening(behandlingId);
        var relevanteOpptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse, sortertPerioder);
        var relevanteOpptjeningInntekter = opptjeningTjeneste.hentRelevanteOpptjeningInntekterForVilkårVurdering(behandlingId, aktørId, sortertFomDatoer);
        boolean brukerHarOppgittAktivtFrilansArbeidsforhold = iayTjeneste.finnGrunnlag(behandlingId)
            .flatMap(InntektArbeidYtelseGrunnlag::getOppgittOpptjening)
            .map(OppgittOpptjening::getFrilans)
            .isPresent();

        //TODO håndter selvstendig næringsdrivende
        for (var vilkårPeriode : sortertPerioder) {
            var stp = vilkårPeriode.getFomDato();
            Opptjening opptjening = opptjeningResultat.finnOpptjening(stp).orElseThrow();
            LocalDate behandlingstidspunkt = LocalDate.now();

            var inntektPerioder = relevanteOpptjeningInntekter.get(stp);
            var aktivitetPerioder = relevanteOpptjeningAktiveter.get(vilkårPeriode);
            var grunnlag = new OpptjeningsgrunnlagAdapter(behandlingstidspunkt, opptjening.getFom(),
                opptjening.getTom()).mapTilGrunnlag(aktivitetPerioder, inntektPerioder);
            grunnlag.setBrukerHarOppgittAktivtFrilansArbeidsforhold(brukerHarOppgittAktivtFrilansArbeidsforhold);

            // TODO(OJR) overstyrer konfig for fp... burde blitt flyttet ut til konfig verdier.. både for FP og for SVP???
            grunnlag.setMinsteAntallDagerGodkjent(28);
            grunnlag.setMinsteAntallMånederGodkjent(0);
            grunnlag.setMinsteAntallDagerForVent(0);
            grunnlag.setMinsteAntallMånederForVent(0);
            grunnlag.setSkalGodkjenneBasertPåAntatt(false);
            // Skrur av sjekk mot inntekt midlertidig
            grunnlag.setSkalValidereMotInntekt(false);

            // returner egen output i tillegg for senere lagring
            OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
            Evaluation evaluation = new Opptjeningsvilkår().evaluer(grunnlag, output);

            VilkårData vilkårData = new VilkårUtfallOversetter().oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag, vilkårPeriode);
            vilkårData.setEkstraVilkårresultat(output);
            alle.put(vilkårPeriode, vilkårData);
        }

        return Collections.unmodifiableNavigableMap(alle);
    }
}
