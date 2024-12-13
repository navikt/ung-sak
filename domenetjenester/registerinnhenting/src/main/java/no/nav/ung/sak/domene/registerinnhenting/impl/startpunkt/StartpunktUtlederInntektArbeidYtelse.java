package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.typer.Saksnummer;

@ApplicationScoped
@GrunnlagRef(InntektArbeidYtelseGrunnlag.class)
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
class StartpunktUtlederInntektArbeidYtelse implements EndringStartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    public StartpunktUtlederInntektArbeidYtelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste iayTjeneste,
                                         @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.iayTjeneste = iayTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object oppdatertGrunnlag, Object forrigeGrunnlag) {
        return hentAlleStartpunktForInntektArbeidYtelse(ref, (UUID) oppdatertGrunnlag, (UUID) forrigeGrunnlag).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<StartpunktType> hentAlleStartpunktForInntektArbeidYtelse(BehandlingReferanse ref, UUID grunnlagId1, UUID grunnlagId2) { // NOSONAR
        List<StartpunktType> startpunkter = new ArrayList<>();
        var oppdatertGrunnlag = grunnlagId1 != null ? iayTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagId1) : null;
        var forrigeGrunnlag = grunnlagId2 != null ? iayTjeneste.hentGrunnlagForGrunnlagId(ref.getBehandlingId(), grunnlagId2) : null;
        var diff = new IAYGrunnlagDiff(oppdatertGrunnlag, forrigeGrunnlag);


        Saksnummer saksnummer = ref.getSaksnummer();

        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.OPPTJENINGSVILKÅRET);

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            var opptjeningsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusDays(30), periode.getFomDato());

            boolean aktørYtelseEndring = diff.endringPåAktørYtelseForAktør(saksnummer, opptjeningsperiode, ref.getAktørId());
            if (aktørYtelseEndring) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.BEREGNING, "aktør ytelse andre tema for periode " + opptjeningsperiode);
            } else {
                var relevantInntektsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusMonths(3), periode.getFomDato());
                boolean erAktørInntektEndretForSøker = diff.erEndringPåAktørInntektForAktør(relevantInntektsperiode, ref.getAktørId());
                if (erAktørInntektEndretForSøker) {
                    leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.BEREGNING, "aktør inntekt for periode " + relevantInntektsperiode);
                }
            }
        }

        return startpunkter;
    }

    private void leggTilStartpunkt(List<StartpunktType> startpunkter, UUID grunnlagId1, UUID grunnlagId2, StartpunktType startpunkt, String endringLoggtekst) {
        startpunkter.add(startpunkt);
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(klassenavn, startpunkt, endringLoggtekst, grunnlagId1, grunnlagId2);
    }

}
