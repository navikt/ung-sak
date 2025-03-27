package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.typer.Saksnummer;

@ApplicationScoped
@GrunnlagRef(InntektArbeidYtelseGrunnlag.class)
@FagsakYtelseTypeRef
class StartpunktUtlederInntektArbeidYtelse implements EndringStartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ProsessTriggerPeriodeUtleder periodeUtleder;

    public StartpunktUtlederInntektArbeidYtelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste iayTjeneste,
                                         ProsessTriggerPeriodeUtleder periodeUtleder) {
        this.iayTjeneste = iayTjeneste;
        this.periodeUtleder = periodeUtleder;
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


        var perioderTilVurdering = periodeUtleder.utledTidslinje(ref.getBehandlingId()).filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT))
            .getLocalDateIntervals();

        for (var periode : perioderTilVurdering) {
            var opptjeningsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusDays(30), periode.getFomDato());

            boolean aktørYtelseEndring = diff.endringPåAktørYtelseForAktør(saksnummer, opptjeningsperiode, ref.getAktørId());
            if (aktørYtelseEndring) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.INIT_PERIODER, "aktør ytelse andre tema for periode " + opptjeningsperiode);
            } else {
                var relevantInntektsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusMonths(3), periode.getFomDato());
                boolean erAktørInntektEndretForSøker = diff.erEndringPåAktørInntektForAktør(relevantInntektsperiode, ref.getAktørId());
                if (erAktørInntektEndretForSøker) {
                    leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.INIT_PERIODER, "aktør inntekt for periode " + relevantInntektsperiode);
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
